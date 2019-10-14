/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.ExplicitApiMode
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.reportDiagnosticOnce
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifier
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyPublicApi

class ExplicitApiDeclarationChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        val state = isEnabled(context.languageVersionSettings)
        if (state == ExplicitApiMode.DISABLED) return

        val isApi = (descriptor as? DeclarationDescriptorWithVisibility)?.isEffectivelyPublicApi ?: return
        if (!isApi) return

        checkVisibilityModifier(state, declaration, descriptor, context)
        checkExplicitReturnType(state, declaration, descriptor, context)
    }

    private fun checkVisibilityModifier(
        state: ExplicitApiMode,
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptorWithVisibility,
        context: DeclarationCheckerContext
    ) {
        val modifier = declaration.visibilityModifier()?.node?.elementType as? KtModifierKeywordToken
        if (modifier != null) return

        if (excludeForDiagnostic(descriptor)) return
        val diagnostic =
            if (state == ExplicitApiMode.STRICT)
                Errors.NO_EXPLICIT_VISIBILITY_IN_API_MODE.on(declaration, descriptor)
            else
                Errors.NO_EXPLICIT_VISIBILITY_IN_API_MODE_WARNING.on(declaration, descriptor)
        context.trace.reportDiagnosticOnce(diagnostic)
    }

    private fun checkExplicitReturnType(
        state: ExplicitApiMode,
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
        if (shouldReport) {
            val diagnostic =
                if (state == ExplicitApiMode.STRICT)
                    Errors.NO_EXPLICIT_RETURN_TYPE_IN_API_MODE.on(declaration)
                else
                    Errors.NO_EXPLICIT_RETURN_TYPE_IN_API_MODE_WARNING
                        .on(declaration)
            context.trace.reportDiagnosticOnce(diagnostic)
        }
    }

    /**
     * Exclusion list:
     * 1. Primary constructors of public API classes
     * 2. Members of public API interfaces
     * 3. do not report overrides of public API? effectively, this means 'no report on overrides at all'
     * 4. Getters and setters (because getters can't change visibility and setter-only explicit visibility looks ugly)
     *
     * Do we need something like @PublicApiFile to disable (or invert) this inspection per-file?
     */
    private fun excludeForDiagnostic(descriptor: DeclarationDescriptor): Boolean {
        /* 1. */ if ((descriptor as? ClassConstructorDescriptor)?.isPrimary == true) return true
        val isMemberOfPublicInterface =
            (descriptor.containingDeclaration as? ClassDescriptor)?.let { DescriptorUtils.isInterface(it) && it.effectiveVisibility().publicApi }
        /* 2. */ if (descriptor is CallableDescriptor && isMemberOfPublicInterface == true) return true
        /* 3. */ if ((descriptor as? CallableDescriptor)?.overriddenDescriptors?.isNotEmpty() == true) return true
        /* 4. */ if (descriptor is PropertyAccessorDescriptor) return true
        return false
    }

    companion object {
        fun isEnabled(settings: LanguageVersionSettings): ExplicitApiMode {
            return settings.getFlag(AnalysisFlags.explicitApiMode)
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