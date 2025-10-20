/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extra

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.ExplicitApiMode
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.diagnostics.visibilityModifier
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.context.findClosest
import org.jetbrains.kotlin.fir.analysis.checkers.syntax.FirDeclarationSyntaxChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.toEffectiveVisibility
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.psi.KtDeclaration

object RedundantVisibilityModifierSyntaxChecker : FirDeclarationSyntaxChecker<FirDeclaration, KtDeclaration>() {

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun checkPsiOrLightTree(
        element: FirDeclaration,
        source: KtSourceElement,
    ) {
        if (element is FirPropertyAccessor || element is FirValueParameter) {
            return
        }

        if (element is FirConstructor && element.source?.kind is KtFakeSourceElementKind) {
            return
        }

        when (element) {
            is FirProperty -> checkPropertyAndReport(element)
            else -> {
                val defaultVisibility = element.symbol.resolvedStatus?.defaultVisibility ?: Visibilities.DEFAULT_VISIBILITY
                checkElementAndReport(element, defaultVisibility)
            }
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkPropertyAndReport(
        property: FirProperty,
    ) {
        var setterImplicitVisibility: Visibility? = null

        property.setter?.let { setter ->
            val defaultVisibility = setter.symbol.resolvedStatus.defaultVisibility
            val visibility = setter.implicitVisibility(defaultVisibility)
            setterImplicitVisibility = visibility
            checkElementAndReport(setter, visibility, property.symbol)
        }

        property.getter?.let { getter ->
            checkElementAndReport(getter, getter.symbol.resolvedStatus.defaultVisibility, property.symbol)
        }

        property.backingField?.let { field ->
            checkElementAndReport(field, field.symbol.resolvedStatus.defaultVisibility, property.symbol)
        }

        if (property.canMakeSetterMoreAccessible(setterImplicitVisibility)) {
            return
        }

        checkElementAndReport(property, property.symbol.resolvedStatus.defaultVisibility)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkElementAndReport(
        element: FirDeclaration,
        defaultVisibility: Visibility,
    ) = checkElementAndReport(
        element,
        defaultVisibility,
        context.findClosest()
    )

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkElementAndReport(
        element: FirDeclaration,
        defaultVisibility: Visibility,
        containingDeclarationSymbol: FirBasedSymbol<*>?,
    ) = checkElementWithImplicitVisibilityAndReport(
        element,
        element.implicitVisibility(defaultVisibility),
        containingDeclarationSymbol
    )

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkElementWithImplicitVisibilityAndReport(
        element: FirDeclaration,
        implicitVisibility: Visibility,
        containingDeclarationSymbol: FirBasedSymbol<*>?,
    ) {
        if (element.source?.kind is KtFakeSourceElementKind && !element.isPropertyFromParameter) {
            return
        }

        if (element !is FirMemberDeclaration) {
            return
        }

        val explicitVisibility = element.source?.explicitVisibility
        val isHidden = explicitVisibility.isEffectivelyHiddenBy(implicitVisibility, containingDeclarationSymbol)
        if (isHidden) {
            reportElement(element)
            return
        }

        // In explicit API mode, `public` is explicitly required.
        val explicitApiMode = context.languageVersionSettings.getFlag(AnalysisFlags.explicitApiMode)
        if (explicitApiMode != ExplicitApiMode.DISABLED && explicitVisibility == Visibilities.Public) {
            return
        }

        if (explicitVisibility == implicitVisibility) {
            reportElement(element)
        }
    }

    private val FirElement.isPropertyFromParameter: Boolean
        get() = this is FirProperty && source?.kind == KtFakeSourceElementKind.PropertyFromParameter

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun reportElement(element: FirDeclaration) {
        reporter.reportOn(element.source, FirErrors.REDUNDANT_VISIBILITY_MODIFIER)
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

    context(context: CheckerContext)
    private fun Visibility?.isEffectivelyHiddenBy(implicitVisibility: Visibility, declaration: FirBasedSymbol<*>?): Boolean {
        if (this == null || this == Visibilities.Protected) {
            return false
        }
        val effectiveVisibility = when (declaration) {
            is FirCallableSymbol<*> -> declaration.effectiveVisibility
            is FirClassLikeSymbol<*> -> declaration.effectiveVisibility
            else -> return false
        }
        val containerVisibility = effectiveVisibility.toVisibility()

        if (containerVisibility == Visibilities.Local && this == Visibilities.Internal) {
            return true
        }

        if (declaration is FirClassLikeSymbol) {
            val contextProvider = context.session.typeContext
            val implicitEffectiveVisibility =
                implicitVisibility.toEffectiveVisibility(declaration).lowerBound(effectiveVisibility, contextProvider)
            val explicitEffectiveVisibility =
                this.toEffectiveVisibility(declaration).lowerBound(effectiveVisibility, contextProvider)

            val relation = explicitEffectiveVisibility.relation(
                implicitEffectiveVisibility, contextProvider
            )
            if (relation == EffectiveVisibility.Permissiveness.MORE || relation == EffectiveVisibility.Permissiveness.UNKNOWN) {
                // If explicit visibility allows more than implicit one, it's not "effectively hidden by"
                return false
            }
        }

        val difference = this.compareTo(containerVisibility) ?: return false
        return difference > 0
    }

    context(context: CheckerContext)
    private fun FirDeclaration.implicitVisibility(defaultVisibility: Visibility): Visibility {
        return when {
            this is FirPropertyAccessor
                    && isSetter
                    && context.containingDeclarations.last() is FirClassSymbol
                    && propertySymbol.isOverride -> findPropertyAccessorVisibility(this)

            this is FirPropertyAccessor -> propertySymbol.visibility

            this is FirConstructor -> {
                val classSymbol = this.getContainingClassSymbol()
                if (classSymbol is FirRegularClassSymbol) {
                    when {
                        classSymbol.isSealed -> Visibilities.Protected
                        classSymbol.isEnumClass -> Visibilities.Private
                        else -> defaultVisibility
                    }
                } else {
                    defaultVisibility
                }
            }

            this is FirNamedFunction
                    && context.containingDeclarations.last() is FirClassSymbol
                    && this.isOverride -> findFunctionVisibility(this)

            this is FirProperty
                    && context.containingDeclarations.last() is FirClassSymbol
                    && this.isOverride -> findPropertyVisibility(this)

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

    context(context: CheckerContext)
    private fun findPropertyAccessorVisibility(accessor: FirPropertyAccessor): Visibility {
        val propertySymbol = accessor.propertySymbol
        return findBiggestVisibility { checkVisibility ->
            propertySymbol.processOverriddenPropertiesWithActionSafe { property ->
                checkVisibility(property.setterSymbol ?: property)
            }
        }
    }

    context(context: CheckerContext)
    private fun findPropertyVisibility(property: FirProperty): Visibility {
        return findBiggestVisibility {
            property.symbol.processOverriddenPropertiesWithActionSafe(it)
        }
    }

    context(context: CheckerContext)
    private fun findFunctionVisibility(function: FirNamedFunction): Visibility {
        return findBiggestVisibility {
            function.symbol.processOverriddenFunctionsWithActionSafe(it)
        }
    }
}

val KtSourceElement.explicitVisibility: Visibility?
    get() {
        val visibilityModifier = treeStructure.visibilityModifier(lighterASTNode)
        return (visibilityModifier?.tokenType as? KtModifierKeywordToken)?.toVisibilityOrNull()
    }
