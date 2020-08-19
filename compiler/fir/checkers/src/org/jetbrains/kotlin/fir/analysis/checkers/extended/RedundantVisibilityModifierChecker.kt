/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifier

object RedundantVisibilityModifierChecker : FirBasicDeclarationChecker() {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration is FirConstructor && declaration.source?.kind is FirFakeSourceElementKind) return
        if (declaration.source is FirFakeSourceElement<*>) return
        if (
            declaration !is FirMemberDeclaration
            && !(declaration is FirPropertyAccessor && declaration.visibility == context.containingPropertyVisibility)
        ) return

        val modifiers = declaration.source.getModifierList()
        val visibilityModifier = when (modifiers) {
            is FirPsiModifierList -> modifiers.modifierList.getVisibility()
            is FirLightModifierList -> modifiers.modifiers.firstOrNull { it.token.toVisibilityOrNull() != null }?.token?.toVisibilityOrNull()
            else -> null
        } ?: return
        val implicitVisibility = declaration.implicitVisibility(context)
        val containingMemberDeclaration = context.findClosest<FirMemberDeclaration>()

        val redundantVisibility = when {
            visibilityModifier == implicitVisibility -> implicitVisibility

            modifiers?.modifiers?.any { it.token == KtTokens.INTERNAL_KEYWORD } == true &&
                    containingMemberDeclaration.let { decl ->
                        decl != null && (decl.isLocalMember || modifiers.modifiers.any { it.token == KtTokens.PRIVATE_KEYWORD })
                    } -> Visibilities.INTERNAL

            else -> null
        } ?: return

        if (
            redundantVisibility == Visibilities.PUBLIC
            && declaration is FirProperty
            && modifiers?.modifiers?.any { it.token == KtTokens.OVERRIDE_KEYWORD } == true
            && declaration.isVar
            && declaration.setter?.visibility == Visibilities.PUBLIC
        ) return

        reporter.report(declaration.source.modifierSource, FirErrors.REDUNDANT_VISIBILITY_MODIFIER)
    }

    private fun FirDeclaration.implicitVisibility(context: CheckerContext): Visibility {
        return when {
            this is FirPropertyAccessor && isSetter && status.isOverride -> this.visibility

            this is FirPropertyAccessor -> {
                context.findClosest<FirProperty>()?.visibility ?: Visibilities.DEFAULT_VISIBILITY
            }

            this is FirConstructor -> {
                val clazz = this.getContainingClass(context)
                if (
                    clazz is FirClass
                    && (clazz.classKind == ClassKind.ENUM_CLASS || clazz.modality() == Modality.SEALED)
                ) {
                    return Visibilities.PRIVATE
                } else {
                    Visibilities.DEFAULT_VISIBILITY
                }
            }

            this is FirSimpleFunction
                    && context.containingDeclarations.last() is FirClass<*>
                    && this.isOverride -> findFunctionVisibility(this, context)

            else -> Visibilities.DEFAULT_VISIBILITY
        }
    }

    private fun findFunctionVisibility(function: FirSimpleFunction, context: CheckerContext): Visibility {
        val currentClass = context.findClosestClassOrObject() ?: return Visibilities.UNKNOWN
        val overriddenFunctions = function.overriddenFunctions(currentClass, context)
        var visibility: Visibility = Visibilities.PRIVATE
        for (func in overriddenFunctions) {
            val currentVisibility = func.fir.visibility()
            if (currentVisibility != null) {
                val compareResult = Visibilities.compare(currentVisibility, visibility)
                if (compareResult != null && compareResult > 0) {
                    visibility = currentVisibility
                }
            }
        }

        return visibility
    }

    private fun FirFunction<*>.visibility(): Visibility? {
        (symbol.fir as? FirMemberDeclaration)?.visibility?.let {
            return it
        }

        (symbol.fir as? FirPropertyAccessor)?.visibility?.let {
            return it
        }

        return null
    }

    private val FirMemberDeclaration.isLocalMember: Boolean
        get() = when (this) {
            is FirProperty -> this.isLocal
            is FirRegularClass -> this.isLocal
            is FirSimpleFunction -> this.isLocal
            else -> false
        }

    private val CheckerContext.containingPropertyVisibility
        get() = (this.containingDeclarations.last() as? FirProperty)?.visibility

    private val FirSourceElement?.modifierSource: FirSourceElement?
        get() = when (this) {
            null -> null
            is FirPsiSourceElement<*> -> (psi as? KtModifierListOwner)?.modifierList?.visibilityModifier()?.toFirPsiSourceElement()
            is FirLightSourceElement -> {
                val modifier = this.getModifierList()?.modifiers?.firstOrNull { it.isVisibilityModifier }
                modifier?.source
            }
        }

    private val FirModifier<*>.isVisibilityModifier
        get() = this.token.toVisibilityOrNull() != null
}
