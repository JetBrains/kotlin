/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.syntax

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.AnalysisFlag
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.ExplicitApiMode
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.isLocalDeclaredInBlock
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.getChild
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.utils.effectiveVisibility
import org.jetbrains.kotlin.fir.declarations.utils.isData
import org.jetbrains.kotlin.fir.declarations.utils.isOverride
import org.jetbrains.kotlin.fir.expressions.impl.FirSingleExpressionBlock
import org.jetbrains.kotlin.fir.resolve.transformers.publishedApiEffectiveVisibility
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtDeclaration

object FirExplicitApiDeclarationChecker : FirDeclarationSyntaxChecker<FirDeclaration, KtDeclaration>() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun checkPsiOrLightTree(
        element: FirDeclaration,
        source: KtSourceElement,
    ) {
        val sourceKindIsReal = source.kind is KtRealSourceElementKind
        if (!sourceKindIsReal &&
            source.kind != KtFakeSourceElementKind.PropertyFromParameter || // Fake properties from parameter should be checked for visibility
            element.origin == FirDeclarationOrigin.ScriptCustomization.ResultProperty ||
            element !is FirMemberDeclaration
        ) {
            return
        }

        fun extractState(flag: AnalysisFlag<ExplicitApiMode>): ExplicitApiMode? {
            return context.languageVersionSettings.getFlag(flag).takeUnless { it == ExplicitApiMode.DISABLED }
        }

        val explicitApiState = extractState(AnalysisFlags.explicitApiMode)
        val explicitReturnTypesState = extractState(AnalysisFlags.explicitReturnTypes)
        if (explicitApiState == null && explicitReturnTypesState == null) return
        // Enum entries do not have visibilities
        if (element is FirEnumEntry) return
        if (!element.effectiveVisibility.publicApi && element.publishedApiEffectiveVisibility == null) return

        val containerEffectiveVisibility = when (val lastContainingDeclaration = context.containingDeclarations.lastOrNull()) {
            is FirClassSymbol<*> -> lastContainingDeclaration.effectiveVisibility
            is FirCallableSymbol<*> -> lastContainingDeclaration.effectiveVisibility
            else -> null
        }
        if (containerEffectiveVisibility?.publicApi == false) {
            return
        }

        if (explicitApiState != null) {
            checkVisibilityModifier(explicitApiState, element, source)
        }

        if (sourceKindIsReal && element is FirCallableDeclaration) {
            // Don't check fake property from parameter because they always have an explicit type (otherwise it's a compiler error)
            checkExplicitReturnType(explicitApiState ?: explicitReturnTypesState!!, element, source)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkVisibilityModifier(
        state: ExplicitApiMode,
        declaration: FirMemberDeclaration,
        source: KtSourceElement,
    ) {
        val visibilityModifier = source.getChild(KtNodeTypes.MODIFIER_LIST, depth = 1)?.getChild(KtTokens.VISIBILITY_MODIFIERS)
        if (visibilityModifier != null) return

        if (explicitVisibilityIsNotRequired(declaration)) return
        val factory = if (state == ExplicitApiMode.STRICT)
            FirErrors.NO_EXPLICIT_VISIBILITY_IN_API_MODE
        else
            FirErrors.NO_EXPLICIT_VISIBILITY_IN_API_MODE_WARNING
        reporter.reportOn(source, factory)
    }

    context(context: CheckerContext)
    /**
     * Exclusion list:
     * 1. Primary constructors of public API classes
     * 2. Properties of data classes in public API
     * 3. Overrides of public API. Effectively, this means 'no report on overrides at all'
     * 4. Getters and setters (because getters can't change visibility and setter-only explicit visibility looks ugly)
     * 5. Properties of annotations in public API
     * 6. Value parameter declaration
     * 7. An anonymous function
     * 8. A local named function
     */
    private fun explicitVisibilityIsNotRequired(declaration: FirMemberDeclaration): Boolean {
        return when (declaration) {
            is FirPrimaryConstructor, // 1,
            is FirPropertyAccessor, // 4
            is FirValueParameter, // 6
            is FirAnonymousFunction
                -> true // 7
            is FirCallableDeclaration -> {
                val containingClass = context.containingDeclarations.lastOrNull() as? FirRegularClassSymbol
                // 2, 5
                if (declaration is FirProperty) {
                    if (containingClass != null && (containingClass.isData || containingClass.classKind == ClassKind.ANNOTATION_CLASS)) {
                        return true
                    }
                }

                // 3, 8
                declaration.isOverride || declaration.isLocalDeclaredInBlock
            }
            else -> false
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkExplicitReturnType(
        state: ExplicitApiMode,
        declaration: FirCallableDeclaration,
        source: KtSourceElement,
    ) {
        if (!declaration.returnTypeCheckIsApplicable()) return

        val factory =
            if (state == ExplicitApiMode.STRICT)
                FirErrors.NO_EXPLICIT_RETURN_TYPE_IN_API_MODE
            else
                FirErrors.NO_EXPLICIT_RETURN_TYPE_IN_API_MODE_WARNING
        reporter.reportOn(source, factory)
    }

    private fun FirCallableDeclaration.returnTypeCheckIsApplicable(): Boolean {
        // It's an explicit type, the check always should be skipped
        if (returnTypeRef.source?.kind == KtRealSourceElementKind) return false

        return this is FirProperty ||
                this is FirFunction &&
                // It's allowed to have implicit return type for getters, for setters the return type is always `Unit`.
                // The return type of the outer property is only worth considering.
                this !is FirPropertyAccessor &&
                // Implicit return type can exist only for single-expression functions, unspecified type for regular functions is incorrect.
                body is FirSingleExpressionBlock
    }
}
