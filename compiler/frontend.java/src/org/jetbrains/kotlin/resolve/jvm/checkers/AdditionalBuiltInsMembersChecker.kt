/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.resolve.jvm.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker

object AdditionalBuiltInsMembersCallChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        if (context.languageVersionSettings.supportsFeature(LanguageFeature.AdditionalBuiltInsMembers)) return
        val resultingDescriptor = resolvedCall.resultingDescriptor as? CallableMemberDescriptor ?: return

        if (resultingDescriptor.isAdditionalBuiltInMember()) {
            context.trace.report(Errors.UNSUPPORTED_FEATURE.on(reportOn, LanguageFeature.AdditionalBuiltInsMembers))
        }
    }
}

object AdditionalBuiltInsMemberOverrideDeclarationChecker : DeclarationChecker {
    override fun check(
            declaration: KtDeclaration,
            descriptor: DeclarationDescriptor,
            diagnosticHolder: DiagnosticSink,
            bindingContext: BindingContext,
            languageVersionSettings: LanguageVersionSettings
    ) {
        if (languageVersionSettings.supportsFeature(LanguageFeature.AdditionalBuiltInsMembers)) return
        val resultingDescriptor = descriptor as? CallableMemberDescriptor ?: return
        val overrideKeyword = declaration.modifierList?.getModifier(KtTokens.OVERRIDE_KEYWORD) ?: return

        val overriddenDescriptors = resultingDescriptor.original.overriddenDescriptors
        if (overriddenDescriptors.isNotEmpty() && overriddenDescriptors.all { it.isAdditionalBuiltInMember() }) {
            diagnosticHolder.report(Errors.UNSUPPORTED_FEATURE.on(overrideKeyword, LanguageFeature.AdditionalBuiltInsMembers))
        }
    }
}

private fun CallableMemberDescriptor.isAdditionalBuiltInMember() =
        KotlinBuiltIns.isBuiltIn(this) && this is JavaCallableMemberDescriptor
