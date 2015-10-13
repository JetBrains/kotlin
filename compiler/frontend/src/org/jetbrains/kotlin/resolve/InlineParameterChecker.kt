/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.lexer.JetModifierKeywordToken
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.JetDeclaration
import org.jetbrains.kotlin.psi.JetFunction
import org.jetbrains.kotlin.psi.JetParameter

object InlineParameterChecker : DeclarationChecker {
    override fun check(declaration: JetDeclaration,
                       descriptor: DeclarationDescriptor,
                       diagnosticHolder: DiagnosticSink,
                       bindingContext: BindingContext
    ) {
        if (declaration is JetFunction) {
            val inline = declaration.hasModifier(JetTokens.INLINE_KEYWORD)
            for (parameter in declaration.valueParameters) {
                val parameterDescriptor = bindingContext.get(BindingContext.VALUE_PARAMETER, parameter)
                if (!inline || (parameterDescriptor != null && !KotlinBuiltIns.isFunctionOrExtensionFunctionType(parameterDescriptor.type))) {
                    parameter.reportIncorrectInline(JetTokens.NOINLINE_KEYWORD, diagnosticHolder)
                    parameter.reportIncorrectInline(JetTokens.CROSSINLINE_KEYWORD, diagnosticHolder)
                }
            }
        }
    }

    private fun JetParameter.reportIncorrectInline(modifierToken: JetModifierKeywordToken, diagnosticHolder: DiagnosticSink) {
        val modifier = modifierList?.getModifier(modifierToken)
        modifier?.let {
            diagnosticHolder.report(Errors.ILLEGAL_INLINE_PARAMETER_MODIFIER.on(modifier, modifierToken))
        }
    }
}