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

package org.jetbrains.kotlin.idea.debugger.stepping

import com.intellij.debugger.SourcePosition
import com.intellij.debugger.actions.JvmSmartStepIntoHandler
import com.intellij.debugger.actions.MethodSmartStepTarget
import com.intellij.debugger.actions.SmartStepTarget
import com.intellij.debugger.engine.MethodFilter
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiMethod
import com.intellij.util.Range
import com.intellij.util.containers.OrderedSet
import org.jetbrains.kotlin.builtins.functions.FunctionInvokeDescriptor
import org.jetbrains.kotlin.builtins.isSuspendFunctionType
import org.jetbrains.kotlin.codegen.intrinsics.IntrinsicMethods
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFully
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.load.java.isFromJava
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getParentCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.inline.InlineUtil

class KotlinSmartStepIntoHandler : JvmSmartStepIntoHandler() {

    override fun isAvailable(position: SourcePosition?) = position?.file is KtFile

    override fun findSmartStepTargets(position: SourcePosition): List<SmartStepTarget> {
        val file = position.file

        val elementAtOffset = position.elementAt ?: return emptyList()

        val element = CodeInsightUtils.getTopmostElementAtOffset(elementAtOffset, elementAtOffset.textRange.startOffset) as? KtElement ?:
                      return emptyList()

        val elementTextRange = element.textRange ?: return emptyList()

        val doc = PsiDocumentManager.getInstance(file.project).getDocument(file) ?: return emptyList()

        val lines = Range(doc.getLineNumber(elementTextRange.startOffset), doc.getLineNumber(elementTextRange.endOffset))
        val bindingContext = element.analyzeFully()
        val result = OrderedSet<SmartStepTarget>()

        // TODO support class initializers, local functions, delegated properties with specified type, setter for properties
        element.accept(object: KtTreeVisitorVoid() {
            override fun visitLambdaExpression(lambdaExpression: KtLambdaExpression) {
                recordFunctionLiteral(lambdaExpression.functionLiteral)
            }

            override fun visitNamedFunction(function: KtNamedFunction) {
                if (!recordFunctionLiteral(function)) {
                    super.visitNamedFunction(function)
                }
            }

            private fun recordFunctionLiteral(function: KtFunction): Boolean {
                val context = function.analyze()
                val resolvedCall = function.getParentCall(context).getResolvedCall(context)
                if (resolvedCall != null) {
                    val arguments = resolvedCall.valueArguments
                    for ((param, argument) in arguments) {
                        if (argument.arguments.any { getArgumentExpression(it) == function }) {
                            val resultingDescriptor = resolvedCall.resultingDescriptor
                            val label = KotlinLambdaSmartStepTarget.calcLabel(resultingDescriptor, param.name)
                            result.add(KotlinLambdaSmartStepTarget(
                                    label, function, lines, InlineUtil.isInline(resultingDescriptor), param.type.isSuspendFunctionType))
                            return true
                        }
                    }
                }
                return false
            }

            private fun getArgumentExpression(it: ValueArgument) = (it.getArgumentExpression() as? KtLambdaExpression)?.functionLiteral ?: it.getArgumentExpression()

            override fun visitObjectLiteralExpression(expression: KtObjectLiteralExpression) {
                // skip calls in object declarations
            }

            override fun visitIfExpression(expression: KtIfExpression) {
                expression.condition?.accept(this)
            }

            override fun visitWhileExpression(expression: KtWhileExpression) {
                expression.condition?.accept(this)
            }

            override fun visitDoWhileExpression(expression: KtDoWhileExpression) {
                expression.condition?.accept(this)
            }

            override fun visitForExpression(expression: KtForExpression) {
                expression.loopRange?.accept(this)
            }

            override fun visitWhenExpression(expression: KtWhenExpression) {
                expression.subjectExpression?.accept(this)
            }

            override fun visitArrayAccessExpression(expression: KtArrayAccessExpression) {
                recordFunction(expression)
                super.visitArrayAccessExpression(expression)
            }

            override fun visitUnaryExpression(expression: KtUnaryExpression) {
                recordFunction(expression.operationReference)
                super.visitUnaryExpression(expression)
            }

            override fun visitBinaryExpression(expression: KtBinaryExpression) {
                recordFunction(expression.operationReference)
                super.visitBinaryExpression(expression)
            }

            override fun visitCallExpression(expression: KtCallExpression) {
                val calleeExpression = expression.calleeExpression
                if (calleeExpression != null) {
                    recordFunction(calleeExpression)
                }
                super.visitCallExpression(expression)
            }

            override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
                val resolvedCall = expression.getResolvedCall(bindingContext)
                if (resolvedCall != null) {
                    val propertyDescriptor = resolvedCall.resultingDescriptor
                    if (propertyDescriptor is PropertyDescriptor) {
                        val getterDescriptor = propertyDescriptor.getter
                        if (getterDescriptor != null && !getterDescriptor.isDefault) {
                            val delegatedResolvedCall = bindingContext[BindingContext.DELEGATED_PROPERTY_RESOLVED_CALL, getterDescriptor]
                            if (delegatedResolvedCall == null) {
                                val getter = DescriptorToSourceUtilsIde.getAnyDeclaration(file.project, getterDescriptor)
                                if (getter is KtPropertyAccessor && getter.hasBody()) {
                                    val label = KotlinMethodSmartStepTarget.calcLabel(getterDescriptor)
                                    result.add(KotlinMethodSmartStepTarget(getterDescriptor, label, expression, lines))
                                }
                            }
                            else {
                                val delegatedPropertyGetterDescriptor = delegatedResolvedCall.resultingDescriptor
                                val label = "${propertyDescriptor.name}." + KotlinMethodSmartStepTarget.calcLabel(delegatedPropertyGetterDescriptor)
                                result.add(KotlinMethodSmartStepTarget(delegatedPropertyGetterDescriptor, label, expression, lines))
                            }
                        }
                    }
                }
                super.visitSimpleNameExpression(expression)
            }

            private fun recordFunction(expression: KtExpression) {
                val resolvedCall = expression.getResolvedCall(bindingContext) ?: return

                val descriptor = resolvedCall.resultingDescriptor
                if (descriptor is FunctionDescriptor && !isIntrinsic(descriptor)) {
                    if (descriptor.isFromJava) {
                        (DescriptorToSourceUtilsIde.getAnyDeclaration(file.project, descriptor) as? PsiMethod)?.let {
                            result.add(MethodSmartStepTarget(it, null, expression, false, lines))
                        }
                    }
                    else {
                        if (descriptor is ConstructorDescriptor && descriptor.isPrimary) {
                            val psiElement = DescriptorToSourceUtilsIde.getAnyDeclaration(file.project, descriptor)
                            if (psiElement is KtClass && psiElement.getAnonymousInitializers().isEmpty()) {
                                // There is no constructor or init block, so do not show it in smart step into
                                return
                            }
                        }

                        val callLabel = KotlinMethodSmartStepTarget.calcLabel(descriptor)
                        val label = when (descriptor) {
                            is FunctionInvokeDescriptor -> {
                                when (expression) {
                                    is KtSimpleNameExpression -> "${runReadAction { expression.text }}.$callLabel"
                                    else -> callLabel
                                }
                            }
                            else -> callLabel
                        }

                        result.add(KotlinMethodSmartStepTarget(descriptor, label, expression, lines))
                    }
                }
            }
        }, null)

        return result
    }

    override fun createMethodFilter(stepTarget: SmartStepTarget?): MethodFilter? {
        return when (stepTarget) {
            is KotlinMethodSmartStepTarget ->
                KotlinBasicStepMethodFilter(stepTarget.descriptor, stepTarget.callingExpressionLines!!)
            is KotlinLambdaSmartStepTarget ->
                KotlinLambdaMethodFilter(
                        stepTarget.getLambda(), stepTarget.callingExpressionLines!!, stepTarget.isInline, stepTarget.isSuspend)
            else -> super.createMethodFilter(stepTarget)
        }
    }


    private val methods = IntrinsicMethods(JvmTarget.JVM_1_6)

    private fun isIntrinsic(descriptor: CallableMemberDescriptor): Boolean {
        return methods.getIntrinsic(descriptor) != null
    }
}
