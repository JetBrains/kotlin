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

import org.jetbrains.kotlin.builtins.getReceiverTypeFromFunctionType
import org.jetbrains.kotlin.builtins.getReturnTypeFromFunctionType
import org.jetbrains.kotlin.builtins.isExtensionFunctionType
import org.jetbrains.kotlin.coroutines.isValidContinuation
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.incremental.KotlinLookupLocation
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.util.OperatorNameConventions

object CoroutineModifierChecker : SimpleDeclarationChecker {
    override fun check(
            declaration: KtDeclaration,
            descriptor: DeclarationDescriptor,
            diagnosticHolder: DiagnosticSink,
            bindingContext: BindingContext
    ) {
        val functionDescriptor = descriptor as? FunctionDescriptor ?: return
        if (declaration !is KtDeclarationWithBody) return
        val location = KotlinLookupLocation(declaration)

        for ((parameterDescriptor, parameterDeclaration) in functionDescriptor.valueParameters.zip(declaration.valueParameters)) {
            if (!parameterDescriptor.isCoroutine) continue
            val coroutineModifier = parameterDeclaration.modifierList?.getModifier(KtTokens.COROUTINE_KEYWORD) ?: continue

            fun report(message: String) {
                diagnosticHolder.report(Errors.INAPPLICABLE_MODIFIER.on(coroutineModifier, KtTokens.COROUTINE_KEYWORD, message))
            }

            if (!parameterDescriptor.type.isExtensionFunctionType) {
                report("parameter should have function type with extension like 'Controller.() -> Continuation<Unit>'")
                continue
            }

            val returnType = parameterDescriptor.type.getReturnTypeFromFunctionType()

            if (returnType.isMarkedNullable || !returnType.isValidContinuation() || !returnType.arguments.single().type.isUnit()) {
                report("parameter should have function type like 'Controller.() -> Continuation<Unit>' (Continuation<Unit> for return type is necessary)")
                continue
            }

            if (functionDescriptor.isInline && !parameterDescriptor.isNoinline) {
                report("coroutine parameter of inline function should be marked as 'noinline'")
                continue
            }

            val controller = parameterDescriptor.type.getReceiverTypeFromFunctionType()!!

            val handleResultFunctions =
                    controller.memberScope
                            .getContributedFunctions(OperatorNameConventions.COROUTINE_HANDLE_RESULT, location)
                            .filter { it.isOperator }

            if (handleResultFunctions.size > 1) {
                report("only one operator handleResult should be declared for controller, but ${handleResultFunctions.size} found")
                continue
            }
        }
    }
}
