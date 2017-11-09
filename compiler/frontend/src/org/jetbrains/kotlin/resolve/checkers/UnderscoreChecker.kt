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

package org.jetbrains.kotlin.resolve.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.descriptors.impl.FunctionExpressionDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext

object UnderscoreChecker : DeclarationChecker {

    @JvmOverloads
    fun checkIdentifier(
            identifier: PsiElement?,
            diagnosticHolder: DiagnosticSink,
            languageVersionSettings: LanguageVersionSettings,
            allowSingleUnderscore: Boolean = false
    ) {
        if (identifier == null || identifier.text.isEmpty()) return
        val isValidSingleUnderscore = allowSingleUnderscore && identifier.text == "_"
        if (!isValidSingleUnderscore && identifier.text.all { it == '_' }) {
            diagnosticHolder.report(Errors.UNDERSCORE_IS_RESERVED.on(identifier))
        }
        else if (isValidSingleUnderscore && !languageVersionSettings.supportsFeature(LanguageFeature.SingleUnderscoreForParameterName)) {
            diagnosticHolder.report(Errors.UNSUPPORTED_FEATURE.on(identifier, LanguageFeature.SingleUnderscoreForParameterName to languageVersionSettings))
        }
    }

    @JvmOverloads
    fun checkNamed(
            declaration: KtNamedDeclaration,
            diagnosticHolder: DiagnosticSink,
            languageVersionSettings: LanguageVersionSettings,
            allowSingleUnderscore: Boolean = false
    ) {
        checkIdentifier(declaration.nameIdentifier, diagnosticHolder, languageVersionSettings, allowSingleUnderscore)
    }

    override fun check(
            declaration: KtDeclaration,
            descriptor: DeclarationDescriptor,
            diagnosticHolder: DiagnosticSink,
            bindingContext: BindingContext,
            languageVersionSettings: LanguageVersionSettings
    ) {
        if (declaration is KtProperty && descriptor !is VariableDescriptor) return
        if (declaration is KtCallableDeclaration) {
            for (parameter in declaration.valueParameters) {
                checkNamed(
                        parameter, diagnosticHolder, languageVersionSettings,
                        allowSingleUnderscore = descriptor is FunctionExpressionDescriptor
                )
            }
        }
        if (declaration is KtTypeParameterListOwner) {
            for (typeParameter in declaration.typeParameters) {
                checkNamed(typeParameter, diagnosticHolder, languageVersionSettings)
            }
        }
        if (declaration !is KtNamedDeclaration) return
        checkNamed(declaration, diagnosticHolder, languageVersionSettings)
    }
}
