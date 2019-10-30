/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.ExplicitApiMode
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.reportDiagnosticOnce
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifier
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyPublicApi

class ExplicitApiDeclarationChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        val state = context.languageVersionSettings.getFlag(AnalysisFlags.explicitApiMode)
        if (state == ExplicitApiMode.DISABLED) return

        if (descriptor !is DeclarationDescriptorWithVisibility) return
        if (!descriptor.isEffectivelyPublicApi) return

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

        if (excludeForDiagnostic(descriptor)) return
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

    /**
     * Exclusion list:
     * 1. Primary constructors of public API classes
     * 2. Properties of data classes in public API
     * 3. Overrides of public API. Effectively, this means 'no report on overrides at all'
     * 4. Getters and setters (because getters can't change visibility and setter-only explicit visibility looks ugly)
     *
     * Do we need something like @PublicApiFile to disable (or invert) this inspection per-file?
     */
    private fun excludeForDiagnostic(descriptor: DeclarationDescriptor): Boolean {
        /* 1. */ if ((descriptor as? ClassConstructorDescriptor)?.isPrimary == true) return true
        /* 2. */ if (descriptor is PropertyDescriptor && (descriptor.containingDeclaration as? ClassDescriptor)?.isData == true) return true
        /* 3. */ if ((descriptor as? CallableDescriptor)?.overriddenDescriptors?.isNotEmpty() == true) return true
        /* 4. */ if (descriptor is PropertyAccessorDescriptor) return true
        return false
    }

    companion object {
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
            return (checkForPublicApi && visibility?.isPublicAPI == true) || (checkForInternal && visibility == Visibilities.INTERNAL) ||
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