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

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.types.expressions.OperatorConventions.REM_TO_MOD_OPERATION_NAMES
import org.jetbrains.kotlin.util.CheckResult
import org.jetbrains.kotlin.util.OperatorChecks
import org.jetbrains.kotlin.util.OperatorNameConventions

object OperatorModifierChecker {
    fun check(
            declaration: KtDeclaration,
            descriptor: DeclarationDescriptor,
            diagnosticHolder: DiagnosticSink,
            languageVersionSettings: LanguageVersionSettings
    ) {
        val functionDescriptor = descriptor as? FunctionDescriptor ?: return
        if (!functionDescriptor.isOperator) return
        val modifier = declaration.modifierList?.getModifier(KtTokens.OPERATOR_KEYWORD) ?: return

        val checkResult = OperatorChecks.check(functionDescriptor)
        if (checkResult.isSuccess) {
            when (functionDescriptor.name) {
                in COROUTINE_OPERATOR_NAMES -> {
                    if (!languageVersionSettings.supportsFeature(LanguageFeature.Coroutines)) {
                        diagnosticHolder.report(Errors.UNSUPPORTED_FEATURE.on(modifier, LanguageFeature.Coroutines))
                    }
                }

                in REM_TO_MOD_OPERATION_NAMES.keys -> {
                    if (!languageVersionSettings.supportsFeature(LanguageFeature.OperatorRem)) {
                        diagnosticHolder.report(Errors.UNSUPPORTED_FEATURE.on(modifier, LanguageFeature.OperatorRem))
                    }
                }
            }

            return
        }

        val errorDescription = (checkResult as? CheckResult.IllegalSignature)?.error ?: "illegal function name"

        diagnosticHolder.report(Errors.INAPPLICABLE_OPERATOR_MODIFIER.on(modifier, errorDescription))
    }
}

private val COROUTINE_OPERATOR_NAMES =
        setOf(OperatorNameConventions.COROUTINE_HANDLE_RESULT,
              OperatorNameConventions.COROUTINE_HANDLE_EXCEPTION,
              OperatorNameConventions.COROUTINE_INTERCEPT_RESUME
        )
