/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.isVisible
import org.jetbrains.kotlin.fir.resolve.toClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.canBeNull
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.visibilityChecker
import org.jetbrains.kotlin.name.SpecialNames.NO_NAME_PROVIDED
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.util.OperatorNameConventions

@OptIn(SymbolInternals::class)
sealed class FirExtensionShadowedByMemberChecker(kind: MppCheckerKind) : FirCallableDeclarationChecker(kind) {
    data object Regular : FirExtensionShadowedByMemberChecker(MppCheckerKind.Platform) {
        override fun check(declaration: FirCallableDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
            if (declaration.isExpect) return
            super.check(declaration, context, reporter)
        }
    }

    data object ForExpectDeclaration : FirExtensionShadowedByMemberChecker(MppCheckerKind.Common) {
        override fun check(declaration: FirCallableDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
            if (!declaration.isExpect) return
            super.check(declaration, context, reporter)
        }
    }

    override fun check(declaration: FirCallableDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (
            declaration.hasAnnotation(StandardClassIds.Annotations.HidesMembers, context.session) ||
            declaration.receiverParameter.let { it == null || it.typeRef.coneType.canBeNull(context.session) } ||
            declaration.nameOrSpecialName == NO_NAME_PROVIDED ||
            // A common pattern, KT-70012
            declaration.isActual
        ) {
            return
        }

        val receiverSymbol = declaration.receiverParameter?.typeRef?.coneType
            ?.toClassLikeSymbol(context.session)
            ?.fullyExpandedClass(context.session)
            ?: return
        val scope = receiverSymbol.unsubstitutedScope(context)

        when (declaration) {
            is FirVariable -> {
                // Collect all properties with the given name
                val properties = mutableListOf<FirVariableSymbol<*>>()
                scope.processPropertiesByName(declaration.name) { properties.add(it) }

                val shadowingMember = findFirstSymbolByCondition(
                    condition = { it.isVisible(context) && !it.isExtension },
                    members = properties
                )
                if (shadowingMember != null) {
                    reporter.reportOn(declaration.source, FirErrors.EXTENSION_SHADOWED_BY_MEMBER, shadowingMember, context)
                }
            }
            is FirSimpleFunction -> {
                // Check for direct function shadowing
                val shadowingFunction = findFirstSymbolByCondition(
                    condition = { it.isVisible(context) && it.shadows(declaration.symbol, context) },
                    members = scope.collectFunctionsByName(declaration.name)
                )
                if (shadowingFunction != null) {
                    reporter.reportOn(declaration.source, FirErrors.EXTENSION_SHADOWED_BY_MEMBER, shadowingFunction, context)
                    return
                }

                // Collect all properties with the given name
                val properties = mutableListOf<FirVariableSymbol<*>>()
                scope.processPropertiesByName(declaration.name) { properties.add(it) }

                // Check for property with invoke operator
                for (property in properties) {
                    if (!property.isVisible(context)) continue

                    val returnTypeScope = property.fir.returnTypeRef.coneType
                        .toClassLikeSymbol(context.session)
                        ?.fullyExpandedClass(context.session)
                        ?.unsubstitutedScope(context)
                        ?: continue

                    val invoke = findFirstSymbolByCondition(
                        condition = { it.isVisible(context) && it.isOperator && it.shadows(declaration.symbol, context) },
                        members = returnTypeScope.collectFunctionsByName(OperatorNameConventions.INVOKE)
                    )

                    if (invoke != null) {
                        reporter.reportOn(
                            declaration.source,
                            FirErrors.EXTENSION_FUNCTION_SHADOWED_BY_MEMBER_PROPERTY_WITH_INVOKE,
                            property, invoke,
                            context,
                        )
                        return
                    }
                }
            }
            else -> {
                // Other declarations are not checked for shadowing
            }
        }
    }

    private fun FirCallableSymbol<*>.isVisible(context: CheckerContext): Boolean {
        val useSiteFile = context.containingFile ?: error("No containing file present when running a checker for top-level functions")

        return context.session.visibilityChecker.isVisible(
            this, context.session, useSiteFile, context.containingDeclarations,
            dispatchReceiver = null,
        )
    }

    private inline fun <T> findFirstSymbolByCondition(
        crossinline condition: (T) -> Boolean,
        members: List<T>,
    ): T? = members.firstOrNull { condition(it) }

    private inline fun <T, K> findFirstNotNullSymbol(
        crossinline transform: (T) -> K?,
        members: List<T>,
    ): K? = members.firstNotNullOfOrNull(transform)

    /**
     * See [isExtensionFunctionShadowedByMemberFunction][org.jetbrains.kotlin.resolve.ShadowedExtensionChecker.isExtensionFunctionShadowedByMemberFunction]
     */
    private fun FirFunctionSymbol<*>.shadows(extension: FirFunctionSymbol<*>, context: CheckerContext): Boolean {
        if (isExtension) return false

        if (valueParameterSymbols.size != extension.valueParameterSymbols.size) return false
        if (varargParameterPosition != extension.varargParameterPosition) return false
        if (extension.isOperator && !isOperator) return false
        if (extension.isInfix && !isInfix) return false

        val helper = context.session.declarationOverloadabilityHelper
        val memberSignature = helper.createSignature(this)
        val extensionSignature = helper.createSignatureForPossiblyShadowedExtension(extension)

        return helper.isEquallyOrMoreSpecific(extensionSignature, memberSignature)
    }

    private val FirFunctionSymbol<*>.varargParameterPosition: Int
        get() = valueParameterSymbols.indexOfFirst { it.isVararg }
}
