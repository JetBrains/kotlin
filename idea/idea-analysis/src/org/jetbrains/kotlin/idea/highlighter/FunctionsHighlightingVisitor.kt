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

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.openapi.extensions.Extensions
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.isFunctionTypeOrSubtype
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.highlighter.KotlinHighlightingColors.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.resolve.calls.tasks.isDynamic
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.serialization.deserialization.KOTLIN_SUSPEND_BUILT_IN_FUNCTION_FQ_NAME
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

internal class FunctionsHighlightingVisitor(holder: AnnotationHolder, bindingContext: BindingContext) :
        AfterAnalysisHighlightingVisitor(holder, bindingContext) {

    override fun visitNamedFunction(function: KtNamedFunction) {
        function.nameIdentifier?.let { highlightName(it, FUNCTION_DECLARATION) }

        super.visitNamedFunction(function)
    }

    override fun visitSuperTypeCallEntry(call: KtSuperTypeCallEntry) {
        val calleeExpression = call.calleeExpression
        val typeElement = calleeExpression.typeReference?.typeElement
        if (typeElement is KtUserType) {
            typeElement.referenceExpression?.let { highlightName(it, CONSTRUCTOR_CALL) }
        }
        super.visitSuperTypeCallEntry(call)
    }

    override fun visitBinaryExpression(expression: KtBinaryExpression) {
        if (expression.operationReference.getIdentifier() != null) {
            val resolvedCall = expression.getResolvedCall(bindingContext)
            if (resolvedCall != null) {
                highlightCall(expression.operationReference, resolvedCall)
            }
        }
        super.visitBinaryExpression(expression)
    }

    override fun visitCallExpression(expression: KtCallExpression) {
        val callee = expression.calleeExpression
        val resolvedCall = expression.getResolvedCall(bindingContext)
        if (callee is KtReferenceExpression && callee !is KtCallExpression && resolvedCall != null) {
            highlightCall(callee, resolvedCall)
        }

        super.visitCallExpression(expression)
    }

    private fun highlightCall(callee: PsiElement, resolvedCall: ResolvedCall<out CallableDescriptor>) {
        val calleeDescriptor = resolvedCall.resultingDescriptor

        val key = Extensions.getExtensions(HighlighterExtension.EP_NAME).firstNotNullResult { extension ->
            extension.highlightCall(callee, resolvedCall)
        } ?: when {
            calleeDescriptor.fqNameOrNull() == KOTLIN_SUSPEND_BUILT_IN_FUNCTION_FQ_NAME -> KEYWORD
            calleeDescriptor.isDynamic() -> DYNAMIC_FUNCTION_CALL
            resolvedCall is VariableAsFunctionResolvedCall -> {
                val container = calleeDescriptor.containingDeclaration
                val containedInFunctionClassOrSubclass = container is ClassDescriptor && container.defaultType.isFunctionTypeOrSubtype
                if (containedInFunctionClassOrSubclass)
                    VARIABLE_AS_FUNCTION_CALL
                else
                    VARIABLE_AS_FUNCTION_LIKE_CALL
            }
            calleeDescriptor is ConstructorDescriptor -> CONSTRUCTOR_CALL
            calleeDescriptor !is FunctionDescriptor -> null
            calleeDescriptor.extensionReceiverParameter != null -> EXTENSION_FUNCTION_CALL
            DescriptorUtils.isTopLevelDeclaration(calleeDescriptor) -> PACKAGE_FUNCTION_CALL
            else -> FUNCTION_CALL
        }
        if (key != null) {
            highlightName(callee, key)
        }
    }
}
