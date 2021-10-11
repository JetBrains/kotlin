/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.syntax

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.ExplicitApiMode
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.FirRealSourceElementKind
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.isLocalMember
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.getChild
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.utils.effectiveVisibility
import org.jetbrains.kotlin.fir.declarations.utils.isData
import org.jetbrains.kotlin.fir.declarations.utils.isOverride
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.transformers.publishedApiEffectiveVisibility
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtDeclaration

object FirExplicitApiDeclarationChecker : FirDeclarationSyntaxChecker<FirDeclaration, KtDeclaration>() {
    private val codeFragmentTypes =
        setOf(KtNodeTypes.BLOCK_CODE_FRAGMENT, KtNodeTypes.EXPRESSION_CODE_FRAGMENT, KtNodeTypes.TYPE_CODE_FRAGMENT)

    override fun checkLightTree(element: FirDeclaration, source: FirSourceElement, context: CheckerContext, reporter: DiagnosticReporter) {
        if ((source.kind !is FirRealSourceElementKind && source.kind != FirFakeSourceElementKind.PropertyFromParameter) ||
            element !is FirMemberDeclaration
        ) {
            return
        }
        val state = context.session.languageVersionSettings.getFlag(AnalysisFlags.explicitApiMode)
        if (state == ExplicitApiMode.DISABLED) return
        // Enum entries do not have visibilities
        if (element is FirEnumEntry) return
        if (!element.effectiveVisibility.publicApi && element.publishedApiEffectiveVisibility == null) return

        checkVisibilityModifier(state, element, source, context, reporter)
        checkExplicitReturnType(state, element, source, context, reporter)
    }

    private fun checkVisibilityModifier(
        state: ExplicitApiMode,
        declaration: FirMemberDeclaration,
        source: FirSourceElement,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        val visibilityModifier = source.getChild(KtNodeTypes.MODIFIER_LIST)?.getChild(KtTokens.VISIBILITY_MODIFIERS)
        if (visibilityModifier != null) return

        if (explicitVisibilityIsNotRequired(declaration, context)) return
        val factory = if (state == ExplicitApiMode.STRICT)
            FirErrors.NO_EXPLICIT_VISIBILITY_IN_API_MODE
        else
            FirErrors.NO_EXPLICIT_VISIBILITY_IN_API_MODE_WARNING
        reporter.reportOn(source, factory, context)
    }

    /**
     * Exclusion list:
     * 1. Primary constructors of public API classes
     * 2. Properties of data classes in public API
     * 3. Overrides of public API. Effectively, this means 'no report on overrides at all'
     * 4. Getters and setters (because getters can't change visibility and setter-only explicit visibility looks ugly)
     * 5. Properties of annotations in public API
     * 6. Value parameter declaration
     *
     * TODO: Do we need something like @PublicApiFile to disable (or invert) this inspection per-file?
     */
    private fun explicitVisibilityIsNotRequired(declaration: FirMemberDeclaration, context: CheckerContext): Boolean {
        return when (declaration) {
            // 1,
            is FirPrimaryConstructor,
            // 4
            is FirPropertyAccessor,
            // 6
            is FirValueParameter -> true
            is FirCallableDeclaration -> {
                val containingClass = context.containingDeclarations.lastOrNull() as? FirRegularClass
                // 2, 5
                if (declaration is FirProperty &&
                    containingClass != null &&
                    (containingClass.isData || containingClass.classKind == ClassKind.ANNOTATION_CLASS)
                ) {
                    return true
                }

                // 3
                declaration.isOverride
            }
            else -> false
        }
    }

    private fun checkExplicitReturnType(
        state: ExplicitApiMode,
        declaration: FirMemberDeclaration,
        source: FirSourceElement,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        if (declaration !is FirCallableDeclaration) return
        if (!returnTypeCheckIsApplicable(source, context)) return

        val shouldReport = returnTypeRequired(declaration, context)
        if (shouldReport) {
            val factory =
                if (state == ExplicitApiMode.STRICT)
                    FirErrors.NO_EXPLICIT_RETURN_TYPE_IN_API_MODE
                else
                    FirErrors.NO_EXPLICIT_RETURN_TYPE_IN_API_MODE_WARNING
            reporter.reportOn(source, factory, context)
        }
    }

    private fun returnTypeCheckIsApplicable(source: FirSourceElement, context: CheckerContext): Boolean {
        // Note that by default getChild uses `depth = -1`, which would find all descendents.
        if (source.getChild(KtNodeTypes.TYPE_REFERENCE, depth = 1) != null) return false
        // Do not check if the containing file is not a physical file.
        val containingFile = context.containingDeclarations.first()
        if (containingFile.source?.elementType in codeFragmentTypes) return false

        return when (source.elementType) {
            // Only require return type if the function is defined via `=`. If it has a block body or is abstract, we don't require return
            // type because not declaring means it returns `Unit`.
            KtNodeTypes.FUN -> source.getChild(KtTokens.EQ, depth = 1) != null
            KtNodeTypes.PROPERTY -> true
            else -> false
        }
    }

    private fun returnTypeRequired(declaration: FirCallableDeclaration, context: CheckerContext): Boolean {
        // If current declaration is local or it's a member in a local declaration (local class, etc), then we do not require return type.
        return !declaration.isLocalMember && context.containingDeclarations.lastOrNull()?.isLocalMember != true
    }
}