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

package org.jetbrains.kotlin.idea.search.usagesSearch.operators

import com.intellij.openapi.components.ServiceManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.SearchRequestCollector
import com.intellij.psi.search.SearchScope
import com.intellij.util.Processor
import org.jetbrains.kotlin.idea.references.KtInvokeFunctionReference
import org.jetbrains.kotlin.idea.search.ideaExtensions.KotlinReferencesSearchOptions
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.psiUtil.isExtensionDeclaration
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UastContext
import org.jetbrains.uast.convertOpt

class InvokeOperatorReferenceSearcher(
    targetFunction: PsiElement,
    searchScope: SearchScope,
    consumer: Processor<in PsiReference>,
    optimizer: SearchRequestCollector,
    options: KotlinReferencesSearchOptions
) : OperatorReferenceSearcher<KtCallExpression>(targetFunction, searchScope, consumer, optimizer, options, wordsToSearch = emptyList()) {
    private val callArgumentsSize: Int?

    init {
        val uastContext = ServiceManager.getService<UastContext>(targetFunction.project, UastContext::class.java)
        callArgumentsSize = when {
            uastContext != null -> {
                val uMethod = uastContext.convertOpt<UMethod>(targetDeclaration, null)
                val uastParameters = uMethod?.uastParameters

                if (uastParameters != null) {
                    val isStableNumberOfArguments = uastParameters.none { uParameter ->
                        @Suppress("UElementAsPsi")
                        uParameter.uastInitializer != null || uParameter.isVarArgs
                    }

                    if (isStableNumberOfArguments) {
                        val numberOfArguments = uastParameters.size
                        when {
                            targetFunction.isExtensionDeclaration() -> numberOfArguments - 1
                            else -> numberOfArguments
                        }
                    } else {
                        null
                    }
                } else {
                    null
                }
            }

            else -> null
        }
    }

    override fun processPossibleReceiverExpression(expression: KtExpression) {
        val callExpression = expression.parent as? KtCallExpression ?: return
        processReferenceElement(callExpression)
    }

    override fun isReferenceToCheck(ref: PsiReference) = ref is KtInvokeFunctionReference

    override fun extractReference(element: KtElement): PsiReference? {
        val callExpression = element as? KtCallExpression ?: return null

        if (callArgumentsSize != null && callArgumentsSize != callExpression.valueArguments.size) {
            return null
        }

        return callExpression.references.firstIsInstance<KtInvokeFunctionReference>()
    }
}