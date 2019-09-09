/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.ApiMode
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.reportDiagnosticOnce
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifier
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyPublicApi

class ApiModeDeclarationChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        val state = isEnabled(context.languageVersionSettings)
        if (state == ApiMode.DISABLED) return

        val isApi = (descriptor as? DeclarationDescriptorWithVisibility)?.isEffectivelyPublicApi ?: return
        if (!isApi) return

        checkVisibilityModifier(state, declaration, descriptor, context)
        checkExplicitReturnType(state, declaration, descriptor, context)
    }

    private fun checkVisibilityModifier(
        state: ApiMode,
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext
    ) {
        val modifier = declaration.visibilityModifier()?.node?.elementType as? KtModifierKeywordToken
        if (modifier != null) return

        if (excludeForDiagnostic(descriptor)) return
        context.selectDiagnosticAndReport(
            declaration,
            state,
            Errors.NO_EXPLICIT_VISIBILITY_IN_API_MODE,
            Errors.NO_EXPLICIT_VISIBILITY_IN_API_MODE_MIGRATION
        )
    }

    private fun checkExplicitReturnType(
        state: ApiMode,
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext
    ) {
        if (declaration !is KtCallableDeclaration) return
        if (!returnTypeCheckIsApplicable(declaration)) return

        val shouldReport = returnTypeRequired(
            declaration, descriptor,
            checkForPublicApi = true,
            checkForInternal = false,
            checkForPrivate = false
        )
        if (shouldReport) context.selectDiagnosticAndReport(
            declaration,
            state,
            Errors.NO_EXPLICIT_RETURN_TYPE_IN_API_MODE,
            Errors.NO_EXPLICIT_RETURN_TYPE_IN_API_MODE_MIGRATION
        )
    }

    private fun DeclarationCheckerContext.selectDiagnosticAndReport(
        on: KtDeclaration,
        state: ApiMode,
        ifError: DiagnosticFactory0<PsiElement>,
        ifWarning: DiagnosticFactory0<PsiElement>
    ) {
        val diagnostic =
            if (state == ApiMode.ENABLED)
                ifError.on(on)
            else
                ifWarning.on(on)
        trace.reportDiagnosticOnce(diagnostic)
    }

    /**
     * Exclusion list:
     * 1. Primary constructors of public API classes
     * 3. Members of public API interfaces
     * 4. do not report overrides of public API? effectively, this means 'no report on overrides at all'
     *
     * Do we need something like @PublicApiFile to disable (or invert) this inspection per-file?
     */
    private fun excludeForDiagnostic(descriptor: DeclarationDescriptor): Boolean {
        /* 1. */ if ((descriptor as? ClassConstructorDescriptor)?.isPrimary == true) return true
        val isMemberOfPublicInterface =
            (descriptor.containingDeclaration as? ClassDescriptor)?.let { DescriptorUtils.isInterface(it) && it.effectiveVisibility().publicApi }
        /* 3. */ if (descriptor is CallableDescriptor && isMemberOfPublicInterface == true) return true
        /* 4. */ if ((descriptor as? CallableDescriptor)?.overriddenDescriptors?.isNotEmpty() == true) return true
        return false
    }

    companion object {
        fun isEnabled(settings: LanguageVersionSettings): ApiMode {
            return settings.getFlag(AnalysisFlags.apiMode)
        }

        fun returnTypeRequired(
            element: KtCallableDeclaration,
            descriptor: DeclarationDescriptor?,
            checkForPublicApi: Boolean,
            checkForInternal: Boolean,
            checkForPrivate: Boolean
        ): Boolean =
            element.containingClassOrObject?.isLocal != true &&
                    when (element) {
                        is KtFunction -> !element.isLocal
                        is KtProperty -> !element.isLocal
                        else -> false
                    } && run {
                val callableMemberDescriptor = descriptor as? CallableMemberDescriptor

                val visibility = callableMemberDescriptor?.effectiveVisibility()?.toVisibility()
                (checkForPublicApi && visibility?.isPublicAPI == true) || (checkForInternal && visibility == Visibilities.INTERNAL) ||
                        (checkForPrivate && visibility == Visibilities.PRIVATE)
            }

        fun returnTypeCheckIsApplicable(element: KtCallableDeclaration): Boolean {
            if (element.containingFile is KtCodeFragment) return false
            if (element is KtFunctionLiteral) return false // TODO(Mikhail Glukhikh): should KtFunctionLiteral be KtCallableDeclaration at all?
            if (element is KtConstructor<*>) return false
            if (element.typeReference != null) return false

            if (element is KtNamedFunction && element.hasBlockBody()) return false

            return true
        }
    }
}