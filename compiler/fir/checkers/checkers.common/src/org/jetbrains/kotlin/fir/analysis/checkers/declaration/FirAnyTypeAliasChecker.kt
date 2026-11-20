/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.annotationPlatformSupport
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.resolve.toTypeParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*

object FirAnyTypeAliasChecker : FirTypeAliasChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirTypeAlias) {
        if (!context.isTopLevel) {
            declaration.requireFeatureSupport(if (declaration.isLocal) LanguageFeature.LocalTypeAliases else LanguageFeature.NestedTypeAliases)

            if (declaration.isExpect) {
                /**
                 * Report it only on nested type aliases to avoid multiple errors that prohibit similar things.
                 * [FirErrors.WRONG_MODIFIER_TARGET] is always reported on expect top-level type aliases (`expect typealias`).
                 * `expect` is explicit for top-level declaration but is not for nested ones.
                 */
                reporter.reportOn(declaration.source, FirErrors.EXPECTED_TYPEALIAS)
            }
        }

        val expandedTypeRef = declaration.expandedTypeRef
        val fullyExpandedType = expandedTypeRef.coneType.fullyExpandedType()

        declaration.checkTypeAliasExpansionCapturesOuterTypeParameters(fullyExpandedType, expandedTypeRef)

        checkForbiddenExpansion(fullyExpandedType, expandedTypeRef.source, expandedTypeRef.coneType.toSymbol() is FirTypeAliasSymbol)
        checkTypeRefForUnderscore(expandedTypeRef)

        val allowNullableNothing = LanguageFeature.NullableNothingInReifiedPosition.isEnabled()
        if (fullyExpandedType.isMalformedExpandedType(allowNullableNothing)) {
            reporter.reportOn(
                declaration.expandedTypeRef.source,
                FirErrors.TYPEALIAS_EXPANDS_TO_ARRAY_OF_NOTHINGS,
                fullyExpandedType
            )
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    fun checkForbiddenExpansion(fullyExpandedType: ConeKotlinType, source: KtSourceElement?, expandsToTypeAlias: Boolean) {
        if (fullyExpandedType.hasError()) return
        if (fullyExpandedType is ConeUnionType) {
            checkForbiddenExpansion(fullyExpandedType.primaryType, source, expandsToTypeAlias)
            fullyExpandedType.richErrorTypes.forEach { checkForbiddenExpansion(it, source, expandsToTypeAlias) }
            return
        }

        val expandedClassSymbol = fullyExpandedType.toRegularClassSymbol()

        if (expandedClassSymbol?.classKind == ClassKind.ANNOTATION_CLASS &&
            context.session.annotationPlatformSupport.requiredAnnotations.contains(expandedClassSymbol.classId)
        ) {
            reporter.reportOn(source, FirErrors.TYPEALIAS_EXPANDS_TO_COMPILER_REQUIRED_ANNOTATION, expandedClassSymbol)
        }

        // Skip reporting if RHS is a type alias to keep the amount of error minimal
        if (!expandsToTypeAlias && expandedClassSymbol == null || fullyExpandedType is ConeDynamicType) {
            reporter.reportOn(source, FirErrors.TYPEALIAS_SHOULD_EXPAND_TO_CLASS, fullyExpandedType)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun FirTypeAlias.checkTypeAliasExpansionCapturesOuterTypeParameters(
        fullyExpandedType: ConeKotlinType,
        expandedTypeRef: FirTypeRef,
    ) {
        if (context.isTopLevel || isInner) return

        val unsubstitutedOuterTypeParameters = mutableSetOf<FirTypeParameterSymbol>()

        fun checkRecursively(coneType: ConeKotlinType) {
            if (coneType is ConeUnionType) {
                checkRecursively(coneType.primaryType)
                coneType.richErrorTypes.forEach(::checkRecursively)
                return
            }

            for (typeArgument in coneType.typeArguments) {
                typeArgument.type?.fullyExpandedType()?.let { checkRecursively(it) }
            }

            val typeParameterSymbol = coneType.toTypeParameterSymbol() ?: return

            if (symbol != typeParameterSymbol.containingDeclarationSymbol) {
                unsubstitutedOuterTypeParameters.add(typeParameterSymbol)
            }
        }

        checkRecursively(fullyExpandedType)

        if (unsubstitutedOuterTypeParameters.isNotEmpty()) {
            reporter.reportOn(
                expandedTypeRef.source,
                FirErrors.TYPEALIAS_EXPANSION_CAPTURES_OUTER_TYPE_PARAMETERS,
                unsubstitutedOuterTypeParameters
            )
        }
    }
}
