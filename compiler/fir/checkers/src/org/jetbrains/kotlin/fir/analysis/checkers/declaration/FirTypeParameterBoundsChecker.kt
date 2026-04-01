/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.utils.isOverride
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeCheckerProviderContext

sealed class FirTypeParameterBoundsChecker(mppKind: MppCheckerKind) : FirTypeParameterChecker(mppKind) {
    object Regular : FirTypeParameterBoundsChecker(MppCheckerKind.Platform) {
        context(context: CheckerContext, reporter: DiagnosticReporter)
        override fun check(declaration: FirTypeParameter) {
            val containingDeclaration = context.containingDeclarations.lastOrNull() ?: return
            if (containingDeclaration.isExpect()) return
            check(declaration, containingDeclaration)
        }
    }

    object ForExpectClass : FirTypeParameterBoundsChecker(MppCheckerKind.Common) {
        context(context: CheckerContext, reporter: DiagnosticReporter)
        override fun check(declaration: FirTypeParameter) {
            val containingDeclaration = context.containingDeclarations.lastOrNull() ?: return
            if (!containingDeclaration.isExpect()) return
            check(declaration, containingDeclaration)
        }
    }

    private val classKinds = setOf(
        ClassKind.CLASS,
        ClassKind.ENUM_CLASS,
        ClassKind.OBJECT
    )

    context(context: CheckerContext, reporter: DiagnosticReporter)
    protected fun check(
        declaration: FirTypeParameter,
        containingDeclaration: FirBasedSymbol<*>,
    ) {
        if (containingDeclaration is FirConstructorSymbol) return

        checkFinalUpperBounds(declaration, containingDeclaration)
        checkExtensionOrContextFunctionTypeBound(declaration)

        if ((containingDeclaration as? FirCallableSymbol)?.isInlineOnly(context.session) != true) {
            checkOnlyOneTypeParameterBound(declaration)
        }

        checkBoundUniqueness(declaration)
        checkConflictingBounds(declaration)
        checkTypeAliasBound(declaration, containingDeclaration)
        checkDynamicBounds(declaration)
        checkInconsistentTypeParameterBounds(declaration)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkFinalUpperBounds(
        declaration: FirTypeParameter,
        containingDeclaration: FirBasedSymbol<*>,
    ) {
        if (containingDeclaration is FirCallableSymbol && containingDeclaration.isOverride) return

        declaration.symbol.resolvedBounds.forEach { bound ->
            val boundType = bound.coneType
            // DYNAMIC_UPPER_BOUND will be reported separately
            if (boundType is ConeDynamicType) return@forEach
            if (!boundType.canHaveSubtypesAccordingToK1(context.session)) {
                reporter.reportOn(bound.source, FirErrors.FINAL_UPPER_BOUND, bound.coneType)
            }
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkExtensionOrContextFunctionTypeBound(
        declaration: FirTypeParameter,
    ) {
        declaration.symbol.resolvedBounds.forEach { bound ->
            if (bound.coneType.fullyExpandedType().unwrapToSimpleTypeUsingLowerBound()
                    .let { it.isExtensionFunctionType || it.hasContextParameters }
            ) {
                reporter.reportOn(bound.source, FirErrors.UPPER_BOUND_IS_EXTENSION_OR_CONTEXT_FUNCTION_TYPE)
            }
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkTypeAliasBound(
        declaration: FirTypeParameter,
        containingDeclaration: FirBasedSymbol<*>,
    ) {
        if (containingDeclaration is FirTypeAliasSymbol) {
            declaration.bounds.filter { it.source?.kind == KtRealSourceElementKind }.forEach { bound ->
                reporter.reportOn(bound.source, FirErrors.BOUND_ON_TYPE_ALIAS_PARAMETER_NOT_ALLOWED)
            }
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkOnlyOneTypeParameterBound(
        declaration: FirTypeParameter,
    ) {
        val bounds = declaration.symbol.resolvedBounds.distinctBy { it.coneType }
        val (boundWithParam, otherBounds) = bounds.partition { it.coneType is ConeTypeParameterType }
        if (boundWithParam.size > 1 || (boundWithParam.size == 1 && otherBounds.isNotEmpty())) {
            // If there's only one problematic bound (either 2 type parameter bounds, or 1 type parameter bound + 1 other bound),
            // report the diagnostic on that bound

            //take TypeConstraint bounds only to report on the same point as old FE
            val constraintBounds = with(SourceNavigator.forElement(declaration)) {
                bounds.filter { it.isInTypeConstraint() }.toSet()
            }
            val reportOn =
                if (bounds.size == 2) {
                    val boundDecl = otherBounds.firstOrNull() ?: boundWithParam.last()
                    if (constraintBounds.contains(boundDecl)) boundDecl.source
                    else declaration.source
                } else {
                    declaration.source
                }
            reporter.reportOn(reportOn, FirErrors.BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkBoundUniqueness(declaration: FirTypeParameter) {
        val seenClasses = mutableSetOf<FirRegularClassSymbol>()
        val allNonErrorBounds = declaration.symbol.resolvedBounds.filter { it !is FirErrorTypeRef }
        val uniqueBounds = allNonErrorBounds.distinctBy { it.coneType.fullyExpandedClassId(context.session) ?: it.coneType }
        val allowUsingClassTypeAsInterface =
            context.session.languageVersionSettings.supportsFeature(LanguageFeature.AllowAnyAsAnActualTypeForExpectInterface)

        uniqueBounds.forEach { bound ->
            val boundConeType = bound.coneType.takeIf { coneType ->
                !allowUsingClassTypeAsInterface || coneType.fullyExpandedType().let { it.abbreviatedType == null || !it.isAnyOrNullableAny }
            }

            boundConeType?.toRegularClassSymbol()?.let { symbol ->
                if (classKinds.contains(symbol.classKind) && seenClasses.add(symbol) && seenClasses.size > 1) {
                    reporter.reportOn(bound.source, FirErrors.ONLY_ONE_CLASS_BOUND_ALLOWED)
                }
            }
        }

        allNonErrorBounds.minus(uniqueBounds).forEach { bound ->
            reporter.reportOn(bound.source, FirErrors.REPEATED_BOUND)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkConflictingBounds(declaration: FirTypeParameter) {
        fun anyConflictingTypes(types: List<ConeKotlinType>): Boolean {
            types.forEach { type ->
                if (!type.canHaveSubtypesAccordingToK1(context.session)) {
                    types.forEach { otherType ->
                        if (type != otherType && !type.isRelated(context.session.typeContext, otherType)) {
                            return true
                        }
                    }
                }
            }
            return false
        }

        if (declaration.bounds.size >= 2 && anyConflictingTypes(declaration.symbol.resolvedBounds.map { it.coneType })) {
            reporter.reportOn(declaration.source, FirErrors.CONFLICTING_UPPER_BOUNDS, declaration.symbol)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkDynamicBounds(declaration: FirTypeParameter) {
        declaration.bounds.forEach { bound ->
            if (bound.coneType is ConeDynamicType) {
                reporter.reportOn(bound.source, FirErrors.DYNAMIC_UPPER_BOUND)
            }
        }
    }

    private fun KotlinTypeMarker.isRelated(context: TypeCheckerProviderContext, type: KotlinTypeMarker?): Boolean =
        isSubtypeOf(context, type) || isSupertypeOf(context, type)

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkInconsistentTypeParameterBounds(
        declaration: FirTypeParameter,
    ) {
        if (declaration.bounds.size <= 1) return

        val firTypeRefClasses = mutableListOf<Pair<FirTypeRef, FirRegularClassSymbol>>()
        val firRegularClassesSet = mutableSetOf<FirRegularClassSymbol>()

        for (bound in declaration.symbol.resolvedBounds) {
            val classSymbol = bound.toRegularClassSymbol(context.session) ?: continue
            if (!firRegularClassesSet.add(classSymbol)) {
                // no need to report INCONSISTENT_TYPE_PARAMETER_BOUNDS because REPEATED_BOUNDS has already been reported
                return
            }

            firTypeRefClasses.add(bound to classSymbol)
        }

        checkInconsistentTypeParameters(firTypeRefClasses, declaration.source, isValues = false)
    }
}
