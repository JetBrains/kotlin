/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.ApiMode
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.reportDiagnosticOnce
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifier
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyPublicApi

class ApiModeDeclarationChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        val state = isEnabled(context.languageVersionSettings)
        if (state == ApiMode.DISABLED) return

        val isApi = (descriptor as? DeclarationDescriptorWithVisibility)?.isEffectivelyPublicApi ?: return
        if (!isApi) return
        val modifier = declaration.visibilityModifier()?.node?.elementType as? KtModifierKeywordToken
        if (modifier != null) return

        if (excludeForDiagnostic(descriptor)) return
        val diagnostic =
            if (state == ApiMode.ENABLED)
                Errors.NO_EXPLICIT_VISIBILITY_IN_API_MODE.on(declaration)
            else
                Errors.NO_EXPLICIT_VISIBILITY_IN_API_MODE_MIGRATION.on(declaration)
        context.trace.reportDiagnosticOnce(diagnostic)
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
    }
}