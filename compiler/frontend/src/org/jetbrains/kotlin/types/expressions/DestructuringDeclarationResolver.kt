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

package org.jetbrains.kotlin.types.expressions

import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtDestructuringDeclarationEntry
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.LocalVariableResolver
import org.jetbrains.kotlin.resolve.TypeResolver
import org.jetbrains.kotlin.resolve.dataClassUtils.createComponentName
import org.jetbrains.kotlin.resolve.scopes.LexicalWritableScope
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.validation.SymbolUsageValidator
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker

class DestructuringDeclarationResolver(
        private val fakeCallResolver: FakeCallResolver,
        private val localVariableResolver: LocalVariableResolver,
        private val typeResolver: TypeResolver,
        private val symbolUsageValidator: SymbolUsageValidator
) {
    fun defineLocalVariablesFromMultiDeclaration(
            writableScope: LexicalWritableScope,
            destructuringDeclaration: KtDestructuringDeclaration,
            receiver: ReceiverValue?,
            initializer: KtExpression?,
            context: ExpressionTypingContext
    ) {
        for ((componentIndex, entry) in destructuringDeclaration.entries.withIndex()) {
            val componentName = createComponentName(componentIndex + 1)

            val componentType = resolveComponentFunctionAndGetType(componentName, context, entry, receiver, initializer)
            val variableDescriptor = localVariableResolver.resolveLocalVariableDescriptorWithType(writableScope, entry, componentType, context.trace)

            ExpressionTypingUtils.checkVariableShadowing(writableScope, context.trace, variableDescriptor)

            writableScope.addVariableDescriptor(variableDescriptor)
        }
    }

    private fun resolveComponentFunctionAndGetType(
            componentName: Name,
            context: ExpressionTypingContext,
            entry: KtDestructuringDeclarationEntry,
            receiver: ReceiverValue?,
            initializer: KtExpression?
    ): KotlinType {
        fun errorType() = ErrorUtils.createErrorType("$componentName() return type")

        if (receiver == null || initializer == null) return errorType()

        val expectedType = getExpectedTypeForComponent(context, entry)
        val results = fakeCallResolver.resolveFakeCall(
                context.replaceExpectedType(expectedType), receiver, componentName,
                entry, initializer, FakeCallKind.COMPONENT, emptyList()
        )

        if (!results.isSuccess) {
            return errorType()
        }

        context.trace.record(BindingContext.COMPONENT_RESOLVED_CALL, entry, results.resultingCall)

        val functionDescriptor = results.resultingDescriptor
        symbolUsageValidator.validateCall(null, functionDescriptor, context.trace, entry)

        val functionReturnType = functionDescriptor.returnType
        if (functionReturnType != null && !TypeUtils.noExpectedType(expectedType)
            && !KotlinTypeChecker.DEFAULT.isSubtypeOf(functionReturnType, expectedType) ) {
            context.trace.report(Errors.COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH.on(initializer, componentName, functionReturnType, expectedType))
        }
        return functionReturnType ?: errorType()
    }

    private fun getExpectedTypeForComponent(context: ExpressionTypingContext, entry: KtDestructuringDeclarationEntry): KotlinType {
        val entryTypeRef = entry.typeReference ?: return TypeUtils.NO_EXPECTED_TYPE
        return typeResolver.resolveType(context.scope, entryTypeRef, context.trace, true)
    }
}
