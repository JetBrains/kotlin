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
import org.jetbrains.kotlin.builtins.isFunctionTypeOrSubtype
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.resolve.calls.tasks.isDynamic

class FunctionsHighlightingVisitor(holder: AnnotationHolder, bindingContext: BindingContext) :
        AfterAnalysisHighlightingVisitor(holder, bindingContext) {

    override fun visitNamedFunction(function: KtNamedFunction) {
        val nameIdentifier = function.nameIdentifier
        if (nameIdentifier != null) {
            NameHighlighter.highlightName(holder, nameIdentifier, KotlinHighlightingColors.FUNCTION_DECLARATION)
        }

        super.visitNamedFunction(function)
    }

    override fun visitSuperTypeCallEntry(call: KtSuperTypeCallEntry) {
        val calleeExpression = call.calleeExpression
        val typeRef = calleeExpression.typeReference
        if (typeRef != null) {
            val typeElement = typeRef.typeElement
            if (typeElement is KtUserType) {
                val nameExpression = typeElement.referenceExpression
                if (nameExpression != null) {
                    NameHighlighter.highlightName(holder, nameExpression, KotlinHighlightingColors.CONSTRUCTOR_CALL)
                }
            }
        }
        super.visitSuperTypeCallEntry(call)
    }

    override fun visitCallExpression(expression: KtCallExpression) {
        val callee = expression.calleeExpression
        val resolvedCall = expression.getResolvedCall(bindingContext)
        if (callee is KtReferenceExpression && resolvedCall != null) {
            val calleeDescriptor = resolvedCall.resultingDescriptor

            if (calleeDescriptor.isDynamic()) {
                NameHighlighter.highlightName(holder, callee, KotlinHighlightingColors.DYNAMIC_FUNCTION_CALL)
            }
            else if (resolvedCall is VariableAsFunctionResolvedCall) {
                val container = calleeDescriptor.containingDeclaration
                val containedInFunctionClassOrSubclass = container is ClassDescriptor && container.defaultType.isFunctionTypeOrSubtype
                NameHighlighter.highlightName(holder, callee, if (containedInFunctionClassOrSubclass)
                    KotlinHighlightingColors.VARIABLE_AS_FUNCTION_CALL
                else
                    KotlinHighlightingColors.VARIABLE_AS_FUNCTION_LIKE_CALL)
            }
            else {
                if (calleeDescriptor is ConstructorDescriptor) {
                    NameHighlighter.highlightName(holder, callee, KotlinHighlightingColors.CONSTRUCTOR_CALL)
                }
                else if (calleeDescriptor is FunctionDescriptor) {
                    NameHighlighter.highlightName(holder, callee, KotlinHighlightingColors.FUNCTION_CALL)
                    if (DescriptorUtils.isTopLevelDeclaration(calleeDescriptor)) {
                        NameHighlighter.highlightName(holder, callee, KotlinHighlightingColors.PACKAGE_FUNCTION_CALL)
                    }
                    if (calleeDescriptor.extensionReceiverParameter != null) {
                        NameHighlighter.highlightName(holder, callee, KotlinHighlightingColors.EXTENSION_FUNCTION_CALL)
                    }
                }
            }
        }

        super.visitCallExpression(expression)
    }
}
