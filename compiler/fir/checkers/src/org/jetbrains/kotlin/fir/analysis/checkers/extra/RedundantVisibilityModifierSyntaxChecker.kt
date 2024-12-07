/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extra

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.ExplicitApiMode
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.diagnostics.visibilityModifier
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.context.findClosest
import org.jetbrains.kotlin.fir.analysis.checkers.findClosestClassOrObject
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.analysis.checkers.resolvedStatus
import org.jetbrains.kotlin.fir.analysis.checkers.syntax.FirDeclarationSyntaxChecker
import org.jetbrains.kotlin.fir.analysis.checkers.toVisibilityOrNull
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.processOverriddenFunctions
import org.jetbrains.kotlin.fir.scopes.processOverriddenProperties
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.psi.KtDeclaration

object RedundantVisibilityModifierSyntaxChecker : FirDeclarationSyntaxChecker<FirDeclaration, KtDeclaration>() {

    override fun checkPsiOrLightTree(
        element: FirDeclaration,
        source: KtSourceElement,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        if (element is FirPropertyAccessor || element is FirValueParameter) {
            return
        }

        if (element is FirConstructor && element.source?.kind is KtFakeSourceElementKind) {
            return
        }

        when (element) {
            is FirProperty -> checkPropertyAndReport(element, context, reporter)
            else -> {
                val defaultVisibility = element.symbol.resolvedStatus?.defaultVisibility ?: Visibilities.DEFAULT_VISIBILITY
                checkElementAndReport(element, defaultVisibility, context, reporter)
            }
        }
    }

    private fun checkPropertyAndReport(
        property: FirProperty,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        var setterImplicitVisibility: Visibility? = null

        property.setter?.let { setter ->
            val defaultVisibility = setter.symbol.resolvedStatus.defaultVisibility
            val visibility = setter.implicitVisibility(context, defaultVisibility)
            setterImplicitVisibility = visibility
            checkElementAndReport(setter, visibility, property, context, reporter)
        }

        property.getter?.let { getter ->
            checkElementAndReport(getter, getter.symbol.resolvedStatus.defaultVisibility, property, context, reporter)
        }

        property.backingField?.let { field ->
            checkElementAndReport(field, field.symbol.resolvedStatus.defaultVisibility, property, context, reporter)
        }

        if (property.canMakeSetterMoreAccessible(setterImplicitVisibility)) {
            return
        }

        checkElementAndReport(property, property.symbol.resolvedStatus.defaultVisibility, context, reporter)
    }

    private fun checkElementAndReport(
        element: FirDeclaration,
        defaultVisibility: Visibility,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) = checkElementAndReport(
        element,
        defaultVisibility,
        context.findClosest(),
        context,
        reporter
    )

    private fun checkElementAndReport(
        element: FirDeclaration,
        defaultVisibility: Visibility,
        containingMemberDeclaration: FirMemberDeclaration?,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) = checkElementWithImplicitVisibilityAndReport(
        element,
        element.implicitVisibility(context, defaultVisibility),
        containingMemberDeclaration,
        context,
        reporter
    )

    private fun checkElementWithImplicitVisibilityAndReport(
        element: FirDeclaration,
        implicitVisibility: Visibility,
        containingMemberDeclaration: FirMemberDeclaration?,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        if (element.source?.kind is KtFakeSourceElementKind && !element.isPropertyFromParameter) {
            return
        }

        if (element !is FirMemberDeclaration) {
            return
        }

        val explicitVisibility = element.source?.explicitVisibility
        val isHidden = explicitVisibility.isEffectivelyHiddenBy(containingMemberDeclaration)
        if (isHidden) {
            reportElement(element, context, reporter)
            return
        }

        // In explicit API mode, `public` is explicitly required.
        val explicitApiMode = context.languageVersionSettings.getFlag(AnalysisFlags.explicitApiMode)
        if (explicitApiMode != ExplicitApiMode.DISABLED && explicitVisibility == Visibilities.Public) {
            return
        }

        if (explicitVisibility == implicitVisibility) {
            reportElement(element, context, reporter)
        }
    }

    private val FirElement.isPropertyFromParameter: Boolean
        get() = this is FirProperty && source?.kind == KtFakeSourceElementKind.PropertyFromParameter

    private fun reportElement(element: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        reporter.reportOn(element.source, FirErrors.REDUNDANT_VISIBILITY_MODIFIER, context)
    }

    private fun FirProperty.canMakeSetterMoreAccessible(setterImplicitVisibility: Visibility?): Boolean {
        if (!isOverride) {
            return false
        }

        if (!hasSetterWithImplicitVisibility) {
            return false
        }

        if (setterImplicitVisibility == null) {
            return false
        }

        return setterImplicitVisibility != visibility
    }

    private val FirProperty.hasSetterWithImplicitVisibility: Boolean
        get() {
            val theSetter = setter ?: return false

            if (source?.lighterASTNode == theSetter.source?.lighterASTNode) {
                return true
            }

            val theSource = theSetter.source ?: return true
            return theSource.explicitVisibility == null
        }

    private fun Visibility?.isEffectivelyHiddenBy(declaration: FirMemberDeclaration?): Boolean {
        if (this == null || this == Visibilities.Protected) {
            return false
        }
        val containerVisibility = declaration?.effectiveVisibility?.toVisibility() ?: return false

        if (containerVisibility == Visibilities.Local && this == Visibilities.Internal) {
            return true
        }

        val difference = this.compareTo(containerVisibility) ?: return false
        return difference > 0
    }

    private fun FirDeclaration.implicitVisibility(context: CheckerContext, defaultVisibility: Visibility): Visibility {
        return when {
            this is FirPropertyAccessor
                    && isSetter
                    && context.containingDeclarations.last() is FirClass
                    && propertySymbol.isOverride -> findPropertyAccessorVisibility(this, context)

            this is FirPropertyAccessor -> propertySymbol.visibility

            this is FirConstructor -> {
                val classSymbol = this.getContainingClassSymbol()
                if (
                    classSymbol is FirRegularClassSymbol
                    && (classSymbol.isEnumClass || classSymbol.isSealed)
                ) {
                    Visibilities.Private
                } else {
                    defaultVisibility
                }
            }

            this is FirSimpleFunction
                    && context.containingDeclarations.last() is FirClass
                    && this.isOverride -> findFunctionVisibility(this, context)

            this is FirProperty
                    && context.containingDeclarations.last() is FirClass
                    && this.isOverride -> findPropertyVisibility(this, context)

            else -> defaultVisibility
        }
    }

    private fun findBiggestVisibility(
        processSymbols: ((FirCallableSymbol<*>) -> ProcessorAction) -> Unit
    ): Visibility {
        var current: Visibility = Visibilities.Private

        processSymbols {
            val difference = Visibilities.compare(current, it.visibility)

            if (difference != null && difference < 0) {
                current = it.visibility
            }

            ProcessorAction.NEXT
        }

        return current
    }

    private fun findPropertyAccessorVisibility(accessor: FirPropertyAccessor, context: CheckerContext): Visibility {
        val containingClass = context.findClosestClassOrObject()?.symbol ?: return Visibilities.Public
        val propertySymbol = accessor.propertySymbol

        val scope = containingClass.unsubstitutedScope(
            context.sessionHolder.session,
            context.sessionHolder.scopeSession,
            withForcedTypeCalculator = false,
            memberRequiredPhase = FirResolvePhase.STATUS,
        )

        return findBiggestVisibility { checkVisibility ->
            scope.processPropertiesByName(propertySymbol.name) {}
            scope.processOverriddenProperties(propertySymbol) { property ->
                checkVisibility(property.setterSymbol ?: property)
            }
        }
    }

    private fun findPropertyVisibility(property: FirProperty, context: CheckerContext): Visibility {
        val containingClass = context.findClosestClassOrObject()?.symbol ?: return Visibilities.Public

        val scope = containingClass.unsubstitutedScope(
            context.sessionHolder.session,
            context.sessionHolder.scopeSession,
            withForcedTypeCalculator = false,
            memberRequiredPhase = FirResolvePhase.STATUS,
        )

        return findBiggestVisibility {
            scope.processPropertiesByName(property.symbol.name) {}
            scope.processOverriddenProperties(property.symbol, it)
        }
    }

    private fun findFunctionVisibility(function: FirSimpleFunction, context: CheckerContext): Visibility {
        val currentClassSymbol = context.findClosestClassOrObject()?.symbol ?: return Visibilities.Unknown

        val scope = currentClassSymbol.unsubstitutedScope(
            context.sessionHolder.session,
            context.sessionHolder.scopeSession,
            withForcedTypeCalculator = false,
            memberRequiredPhase = FirResolvePhase.STATUS,
        )

        return findBiggestVisibility {
            scope.processFunctionsByName(function.symbol.name) {}
            scope.processOverriddenFunctions(function.symbol, it)
        }
    }
}

val KtSourceElement.explicitVisibility: Visibility?
    get() {
        val visibilityModifier = treeStructure.visibilityModifier(lighterASTNode)
        return (visibilityModifier?.tokenType as? KtModifierKeywordToken)?.toVisibilityOrNull()
    }
