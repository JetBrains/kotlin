/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.ExplicitApiMode
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.reportDiagnosticOnce
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifier
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyPublicApi
import org.jetbrains.kotlin.resolve.descriptorUtil.isPublishedApi

class ExplicitApiDeclarationChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        val state = context.languageVersionSettings.getFlag(AnalysisFlags.explicitApiMode)
        if (state == ExplicitApiMode.DISABLED) return

        if (descriptor !is DeclarationDescriptorWithVisibility) return
        if (descriptor is ClassDescriptor && descriptor.kind == ClassKind.ENUM_ENTRY) return // Enum entries does not have visibilities
        if (!descriptor.isEffectivelyPublicApi && !descriptor.isPublishedApi()) return

        checkVisibilityModifier(state, declaration, descriptor, context)
        checkExplicitReturnType(state, declaration, descriptor, context)
    }

    private fun checkVisibilityModifier(
        state: ExplicitApiMode,
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptorWithVisibility,
        context: DeclarationCheckerContext
    ) {
        val modifier = declaration.visibilityModifier()
        if (modifier != null) return

        if (explicitVisibilityIsNotRequired(descriptor)) return
        val diagnostic =
            if (state == ExplicitApiMode.STRICT)
                Errors.NO_EXPLICIT_VISIBILITY_IN_API_MODE
            else
                Errors.NO_EXPLICIT_VISIBILITY_IN_API_MODE_WARNING
        context.trace.reportDiagnosticOnce(diagnostic.on(declaration))
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
                    Errors.NO_EXPLICIT_RETURN_TYPE_IN_API_MODE
                else
                    Errors.NO_EXPLICIT_RETURN_TYPE_IN_API_MODE_WARNING
            context.trace.reportDiagnosticOnce(diagnostic.on(declaration))
        }
    }

    companion object {
        /**
         * Exclusion list:
         * 1. Primary constructors of public API classes
         * 2. Properties of data classes in public API
         * 3. Overrides of public API. Effectively, this means 'no report on overrides at all'
         * 4. Getters and setters (because getters can't change visibility and setter-only explicit visibility looks ugly)
         * 5. Properties of annotations in public API
         *
         * Do we need something like @PublicApiFile to disable (or invert) this inspection per-file?
         */
        fun explicitVisibilityIsNotRequired(descriptor: DeclarationDescriptor): Boolean {
            /* 1. */ if ((descriptor as? ClassConstructorDescriptor)?.isPrimary == true) return true
            /* 2. */ if (descriptor is PropertyDescriptor && (descriptor.containingDeclaration as? ClassDescriptor)?.isData == true) return true
            /* 3. */ if ((descriptor as? CallableDescriptor)?.overriddenDescriptors?.isNotEmpty() == true) return true
            /* 4. */ if (descriptor is PropertyAccessorDescriptor) return true
            /* 5. */ if (descriptor is PropertyDescriptor && (descriptor.containingDeclaration as? ClassDescriptor)?.kind == ClassKind.ANNOTATION_CLASS) return true
            return false
        }

        fun returnTypeRequired(
            element: KtCallableDeclaration,
            descriptor: DeclarationDescriptor?,
            checkForPublicApi: Boolean,
            checkForInternal: Boolean,
            checkForPrivate: Boolean
        ): Boolean {
            if (element.containingClassOrObject?.isLocal == true) return false
            if (element is KtFunction && element.isLocal) return false
            if (element is KtProperty && element.isLocal) return false

            val callableMemberDescriptor = descriptor as? CallableMemberDescriptor

            val visibility = callableMemberDescriptor?.effectiveVisibility()?.toVisibility()
            val isPublicApi =
                visibility?.isPublicAPI == true || (visibility == Visibilities.Internal && callableMemberDescriptor.isPublishedApi())
            return (checkForPublicApi && isPublicApi) || (checkForInternal && visibility == Visibilities.Internal) ||
                    (checkForPrivate && visibility == Visibilities.Internal)
        }

        fun returnTypeCheckIsApplicable(element: KtCallableDeclaration): Boolean {
            if (element.containingFile is KtCodeFragment) return false
            if (element is KtFunctionLiteral) return false // TODO(Mikhail Glukhikh): should KtFunctionLiteral be KtCallableDeclaration at all?
            if (element is KtConstructor<*>) return false
            if (element.typeReference != null) return false

            if (element is KtNamedFunction && element.hasBlockBody()) return false

            return true
        }

        fun publicReturnTypeShouldBePresentInApiMode(
            element: KtCallableDeclaration,
            languageVersionSettings: LanguageVersionSettings,
            descriptor: DeclarationDescriptor?
        ): Boolean {
            val isInApiMode = languageVersionSettings.getFlag(AnalysisFlags.explicitApiMode) != ExplicitApiMode.DISABLED
            return isInApiMode && returnTypeRequired(
                element,
                descriptor,
                checkForPublicApi = true,
                checkForInternal = false,
                checkForPrivate = false
            )
        }
    }
}

val LanguageVersionSettings.explicitApiEnabled: Boolean
    get() = getFlag(AnalysisFlags.explicitApiMode) != ExplicitApiMode.DISABLED