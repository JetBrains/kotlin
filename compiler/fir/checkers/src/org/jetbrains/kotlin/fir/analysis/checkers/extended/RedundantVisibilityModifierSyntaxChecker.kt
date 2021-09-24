/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.context.findClosest
import org.jetbrains.kotlin.fir.analysis.checkers.findClosestClassOrObject
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.analysis.checkers.overriddenFunctions
import org.jetbrains.kotlin.fir.analysis.checkers.syntax.FirDeclarationSyntaxChecker
import org.jetbrains.kotlin.fir.analysis.checkers.toVisibilityOrNull
import org.jetbrains.kotlin.fir.analysis.diagnostics.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.declarations.utils.isEnumClass
import org.jetbrains.kotlin.fir.declarations.utils.isOverride
import org.jetbrains.kotlin.fir.declarations.utils.isSealed
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.scopes.getDirectOverriddenFunctions
import org.jetbrains.kotlin.fir.scopes.getDirectOverriddenProperties
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.psi.KtDeclaration

object RedundantVisibilityModifierSyntaxChecker : FirDeclarationSyntaxChecker<FirDeclaration, KtDeclaration>() {

    override fun checkPsiOrLightTree(
        element: FirDeclaration,
        source: FirSourceElement,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        if (element is FirConstructor && source.kind is FirFakeSourceElementKind) return
        if (source.kind is FirFakeSourceElementKind) return
        if (
            element !is FirMemberDeclaration
            && !(element is FirPropertyAccessor && element.visibility == context.containingPropertyVisibility)
        ) return

        val visibilityModifier = source.treeStructure.visibilityModifier(source.lighterASTNode)
        val explicitVisibility = (visibilityModifier?.tokenType as? KtModifierKeywordToken)?.toVisibilityOrNull()
        val implicitVisibility = element.implicitVisibility(context)
        val containingMemberDeclaration = context.findClosest<FirMemberDeclaration>()

        val isHidden = explicitVisibility.isEffectivelyHiddenBy(containingMemberDeclaration)

        if (explicitVisibility != implicitVisibility && !isHidden) {
            return
        }

        if (element.isPublicOverriddenWithNonPublicBase(containingMemberDeclaration, context)) {
            return
        }

        reporter.reportOn(source, FirErrors.REDUNDANT_VISIBILITY_MODIFIER, context)
    }

    private fun FirDeclaration.isPublicOverriddenWithNonPublicBase(
        container: FirMemberDeclaration?,
        context: CheckerContext,
    ): Boolean {
        if (container !is FirClass) {
            return false
        }

        val scope = container.unsubstitutedScope(context.session, ScopeSession(), false)

        val overridden = when (this) {
            is FirProperty -> scope.getDirectOverriddenProperties(symbol)
            is FirSimpleFunction -> scope.getDirectOverriddenFunctions(symbol)
            else -> return false
        }

        return overridden.any { it.visibility != Visibilities.Public }
    }

    private fun Visibility?.isEffectivelyHiddenBy(declaration: FirMemberDeclaration?): Boolean {
        val containerVisibility = declaration?.effectiveVisibility?.toVisibility() ?: return false

        if (containerVisibility == Visibilities.Local && this == Visibilities.Internal) {
            return true
        }

        val difference = this?.compareTo(containerVisibility) ?: return false
        return difference > 0
    }

    private fun FirDeclaration.implicitVisibility(context: CheckerContext): Visibility {
        return when {
            this is FirPropertyAccessor && isSetter && status.isOverride -> this.visibility

            this is FirPropertyAccessor -> {
                context.findClosest<FirProperty>()?.visibility ?: Visibilities.DEFAULT_VISIBILITY
            }

            this is FirConstructor -> {
                val classSymbol = this.getContainingClassSymbol(context.session)
                if (
                    classSymbol is FirRegularClassSymbol
                    && (classSymbol.isEnumClass || classSymbol.isSealed)
                ) {
                    Visibilities.Private
                } else {
                    Visibilities.DEFAULT_VISIBILITY
                }
            }

            this is FirSimpleFunction
                    && context.containingDeclarations.last() is FirClass
                    && this.isOverride -> findFunctionVisibility(this, context)

            else -> Visibilities.DEFAULT_VISIBILITY
        }
    }

    private fun findFunctionVisibility(function: FirSimpleFunction, context: CheckerContext): Visibility {
        val currentClassSymbol = context.findClosestClassOrObject()?.symbol ?: return Visibilities.Unknown
        val overriddenFunctions = function.overriddenFunctions(currentClassSymbol, context)
        var visibility: Visibility = Visibilities.Private
        for (func in overriddenFunctions) {
            val currentVisibility = func.visibility
            val compareResult = Visibilities.compare(currentVisibility, visibility)
            if (compareResult != null && compareResult > 0) {
                visibility = currentVisibility
            }
        }

        return visibility
    }

    private val CheckerContext.containingPropertyVisibility
        get() = (this.containingDeclarations.last() as? FirProperty)?.visibility
}
