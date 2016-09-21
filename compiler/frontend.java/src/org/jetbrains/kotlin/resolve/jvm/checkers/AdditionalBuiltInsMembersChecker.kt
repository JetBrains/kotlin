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
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor
import org.jetbrains.kotlin.load.java.isFromBuiltins
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DeclarationChecker
import org.jetbrains.kotlin.resolve.descriptorUtil.overriddenTreeUniqueAsSequence

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

        // TODO: allow to omit 'override' on additional built-ins members
        reportErrorIfAdditionalBuiltinDescriptor(resultingDescriptor, diagnosticHolder, overrideKeyword)
    }
}

private fun reportErrorIfAdditionalBuiltinDescriptor(
        descriptor: CallableMemberDescriptor,
        diagnosticHolder: DiagnosticSink,
        reportOn: PsiElement
) {
    @Suppress("UNCHECKED_CAST")
    val overriddenTree = descriptor.overriddenTreeUniqueAsSequence(useOriginal = true) as Sequence<CallableMemberDescriptor>

    if (overriddenTree.any { it.isFromBuiltins() && it is JavaCallableMemberDescriptor }) {
        diagnosticHolder.report(Errors.UNSUPPORTED_FEATURE.on(reportOn, LanguageFeature.AdditionalBuiltInsMembers))
    }
}
