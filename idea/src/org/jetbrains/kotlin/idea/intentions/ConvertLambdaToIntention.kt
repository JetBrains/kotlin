/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.hasDefaultValue

abstract class ConvertLambdaToIntention(text: String) : SelfTargetingOffsetIndependentIntention<KtLambdaExpression>(KtLambdaExpression::class.java, text) {
    abstract fun buildReferenceText(element: KtLambdaExpression): String?

    protected fun KtLambdaArgument.outerCalleeDescriptor(): FunctionDescriptor? {
        val outerCallExpression = parent as? KtCallExpression ?: return null
        val context = outerCallExpression.analyze()
        val outerCallee = outerCallExpression.calleeExpression as? KtReferenceExpression ?: return null
        return context[BindingContext.REFERENCE_TARGET, outerCallee] as? FunctionDescriptor
    }

    override fun applyTo(element: KtLambdaExpression, editor: Editor?) {
        val referenceName = buildReferenceText(element) ?: return
        val factory = KtPsiFactory(element)
        val lambdaArgument = element.parent as? KtLambdaArgument
        if (lambdaArgument == null) {
            // Without lambda argument syntax, just replace lambda with reference
            val callableReferenceExpr = factory.createCallableReferenceExpression(referenceName) ?: return
            (element.replace(callableReferenceExpr) as? KtElement)?.let { ShortenReferences.RETAIN_COMPANION.process(it) }
        }
        else {
            // Otherwise, replace the whole argument list for lambda argument-using call
            val outerCallExpression = lambdaArgument.parent as? KtCallExpression ?: return
            val outerCalleeDescriptor = lambdaArgument.outerCalleeDescriptor() ?: return
            // Parameters with default value
            val valueParameters = outerCalleeDescriptor.valueParameters
            val arguments = outerCallExpression.valueArguments.filter { it !is KtLambdaArgument }
            val useNamedArguments = valueParameters.any { it.hasDefaultValue() } || arguments.any { it.getArgumentName() != null }

            if (useNamedArguments && arguments.size > valueParameters.size) return
            val newArgumentList = factory.buildValueArgumentList {
                appendFixedText("(")
                arguments.forEachIndexed { i, argument ->
                    if (useNamedArguments) {
                        val argumentName = argument.getArgumentName()?.asName
                        val name = argumentName ?: valueParameters[i].name
                        appendName(name)
                        appendFixedText(" = ")
                    }
                    appendExpression(argument.getArgumentExpression())
                    appendFixedText(", ")
                }
                if (useNamedArguments) {
                    appendName(valueParameters.last().name)
                    appendFixedText(" = ")
                }
                appendFixedText(referenceName)
                appendFixedText(")")
            }
            val argumentList = outerCallExpression.valueArgumentList
            if (argumentList == null) {
                (lambdaArgument.replace(newArgumentList) as? KtElement)?.let { ShortenReferences.RETAIN_COMPANION.process(it) }
            }
            else {
                (argumentList.replace(newArgumentList) as? KtValueArgumentList)?.let {
                    ShortenReferences.RETAIN_COMPANION.process(it.arguments.last())
                }
                lambdaArgument.delete()
            }
        }
    }
}