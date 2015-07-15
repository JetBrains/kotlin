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
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.analyzeAndGetResult
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getParentCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.inline.InlineUtil

public class KotlinSmartStepIntoHandler : JvmSmartStepIntoHandler() {

    override fun isAvailable(position: SourcePosition?) = position?.getFile() is JetFile

    override fun findSmartStepTargets(position: SourcePosition): List<SmartStepTarget> {
        if (position.getLine() < 0) return emptyList()

        val file = position.getFile()

        val lineStart = CodeInsightUtils.getStartLineOffset(file, position.getLine()) ?: return emptyList()

        val elementAtOffset = file.findElementAt(lineStart) ?: return emptyList()

        val element = CodeInsightUtils.getTopmostElementAtOffset(elementAtOffset, lineStart)
        if (element !is JetElement) return emptyList()

        val elementTextRange = element.getTextRange() ?: return emptyList()

        val doc = PsiDocumentManager.getInstance(file.getProject()).getDocument(file) ?: return emptyList()

        val lines = Range(doc.getLineNumber(elementTextRange.getStartOffset()), doc.getLineNumber(elementTextRange.getEndOffset()))
        val bindingContext = element.analyze()
        val result = OrderedSet<SmartStepTarget>()

        // TODO support class initializers, local functions, delegated properties with specified type, setter for properties
        element.accept(object: JetTreeVisitorVoid() {

            override fun visitFunctionLiteralExpression(expression: JetFunctionLiteralExpression) {
                val context = expression.analyzeAndGetResult().bindingContext
                val resolvedCall = expression.getParentCall(context).getResolvedCall(context)
                if (resolvedCall != null && !InlineUtil.isInline(resolvedCall.getResultingDescriptor())) {
                    val arguments = resolvedCall.getValueArguments()
                    for ((param, argument) in arguments) {
                        if (argument.getArguments().any { it.getArgumentExpression() == expression}) {
                            val label = KotlinLambdaSmartStepTarget.calcLabel(resolvedCall.getResultingDescriptor(), param.getName())
                            result.add(KotlinLambdaSmartStepTarget(label, expression, lines))
                            break
                        }
                    }
                }
            }

            override fun visitObjectLiteralExpression(expression: JetObjectLiteralExpression) {
                // skip calls in object declarations
            }

            override fun visitIfExpression(expression: JetIfExpression) {
                expression.getCondition()?.accept(this)
            }

            override fun visitWhileExpression(expression: JetWhileExpression) {
                expression.getCondition()?.accept(this)
            }

            override fun visitDoWhileExpression(expression: JetDoWhileExpression) {
                expression.getCondition()?.accept(this)
            }

            override fun visitForExpression(expression: JetForExpression) {
                expression.getLoopRange()?.accept(this)
            }

            override fun visitWhenExpression(expression: JetWhenExpression) {
                expression.getSubjectExpression()?.accept(this)
            }

            override fun visitArrayAccessExpression(expression: JetArrayAccessExpression) {
                recordFunction(expression)
                super.visitArrayAccessExpression(expression)
            }

            override fun visitUnaryExpression(expression: JetUnaryExpression) {
                recordFunction(expression.getOperationReference())
                super.visitUnaryExpression(expression)
            }

            override fun visitBinaryExpression(expression: JetBinaryExpression) {
                recordFunction(expression.getOperationReference())
                super.visitBinaryExpression(expression)
            }

            override fun visitCallExpression(expression: JetCallExpression) {
                val calleeExpression = expression.getCalleeExpression()
                if (calleeExpression != null) {
                    recordFunction(calleeExpression)
                }
                super.visitCallExpression(expression)
            }

            override fun visitSimpleNameExpression(expression: JetSimpleNameExpression) {
                val resolvedCall = expression.getResolvedCall(bindingContext)
                if (resolvedCall != null) {
                    val propertyDescriptor = resolvedCall.getResultingDescriptor()
                    if (propertyDescriptor is PropertyDescriptor) {
                        val getterDescriptor = propertyDescriptor.getGetter()
                        if (getterDescriptor != null && !getterDescriptor.isDefault()) {
                            val delegatedResolvedCall = bindingContext[BindingContext.DELEGATED_PROPERTY_RESOLVED_CALL, getterDescriptor]
                            if (delegatedResolvedCall == null) {
                                val getter = DescriptorToSourceUtilsIde.getAnyDeclaration(file.getProject(), getterDescriptor)
                                if (getter is JetPropertyAccessor && (getter.getBodyExpression() != null || getter.getEqualsToken() != null)) {
                                    val psiMethod = LightClassUtil.getLightClassAccessorMethod(getter)
                                    if (psiMethod != null) {
                                        result.add(KotlinMethodSmartStepTarget(getter, psiMethod, null, expression, false, lines))
                                    }
                                }
                            }
                            else {
                                val delegatedPropertyGetterDescriptor = delegatedResolvedCall.getResultingDescriptor()
                                if (delegatedPropertyGetterDescriptor is CallableMemberDescriptor) {
                                    val function = DescriptorToSourceUtilsIde.getAnyDeclaration(file.getProject(), delegatedPropertyGetterDescriptor)
                                    if (function is JetNamedFunction || function is JetSecondaryConstructor) {
                                        val psiMethod = LightClassUtil.getLightClassMethod(function as JetFunction)
                                        if (psiMethod != null) {
                                            result.add(KotlinMethodSmartStepTarget(function, psiMethod, "${propertyDescriptor.getName()}.", expression, false, lines))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                super.visitSimpleNameExpression(expression)
            }

            private fun recordFunction(expression: JetExpression) {
                val resolvedCall = expression.getResolvedCall(bindingContext) ?: return

                val descriptor = resolvedCall.getResultingDescriptor()
                if (descriptor is CallableMemberDescriptor) {
                    // TODO doesn't work for libraries
                    val function = DescriptorToSourceUtilsIde.getAnyDeclaration(file.getProject(), descriptor)
                    if (function is JetNamedFunction || function is JetSecondaryConstructor) {
                        val psiMethod = LightClassUtil.getLightClassMethod(function as JetFunction)
                        if (psiMethod != null) {
                            result.add(KotlinMethodSmartStepTarget(function, psiMethod, null, expression, false, lines))
                        }
                    }
                    else if (function is PsiMethod) {
                        result.add(MethodSmartStepTarget(function, null, expression, false, lines))
                    }
                }
            }
        }, null)

        return result
    }

    override fun createMethodFilter(stepTarget: SmartStepTarget?): MethodFilter? {
        return when (stepTarget) {
            is KotlinMethodSmartStepTarget -> KotlinBasicStepMethodFilter(stepTarget)
            is KotlinLambdaSmartStepTarget -> KotlinLambdaMethodFilter(stepTarget.getLambda(), stepTarget.getCallingExpressionLines()!! )
            else -> super.createMethodFilter(stepTarget)
        }
    }
}
