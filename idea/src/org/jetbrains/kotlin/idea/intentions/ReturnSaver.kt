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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.util.Key
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.resolve.bindingContextUtil.getTargetFunction
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class ReturnSaver(val function: KtNamedFunction) {
    val RETURN_KEY = Key<Unit>("RETURN_KEY")

    init {
        save()
    }

    private fun save() {
        val body = function.bodyExpression!!
        body.forEachDescendantOfType<KtReturnExpression> {
            if (it.getTargetFunction(it.analyze(BodyResolveMode.PARTIAL)) == function) {
                it.putCopyableUserData(RETURN_KEY, Unit)
            }
        }
    }

    private fun clear() {
        val body = function.bodyExpression!!
        body.forEachDescendantOfType<KtReturnExpression> { it.putCopyableUserData(RETURN_KEY, null) }
    }

    fun restore(lambda: KtLambdaExpression, label: Name) {
        clear()

        val factory = KtPsiFactory(lambda)

        val lambdaBody = lambda.bodyExpression!!

        val returnToReplace = lambda.collectDescendantsOfType<KtReturnExpression>() { it.getCopyableUserData(RETURN_KEY) != null }

        for (returnExpression in returnToReplace) {
            val value = returnExpression.returnedExpression
            val replaceWith = if (value != null && returnExpression.isValueOfBlock(lambdaBody)) {
                value
            }
            else if (value != null) {
                factory.createExpressionByPattern("return@$0 $1", label, value)
            }
            else {
                factory.createExpressionByPattern("return@$0", label)
            }

            returnExpression.replace(replaceWith)

        }
    }

    private fun KtExpression.isValueOfBlock(inBlock: KtBlockExpression): Boolean {
        val parent = parent
        when (parent) {
            inBlock -> {
                return this == inBlock.statements.last()
            }

            is KtBlockExpression -> {
                return isValueOfBlock(parent) && parent.isValueOfBlock(inBlock)
            }

            is KtContainerNode -> {
                val owner = parent.parent
                if (owner is KtIfExpression) {
                    return (this == owner.then || this == owner.`else`) && owner.isValueOfBlock(inBlock)
                }
            }

            is KtWhenEntry -> {
                return this == parent.expression && (parent.parent as KtWhenExpression).isValueOfBlock(inBlock)
            }
        }

        return false
    }

}