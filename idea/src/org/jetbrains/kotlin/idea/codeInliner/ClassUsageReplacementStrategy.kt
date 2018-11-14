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

package org.jetbrains.kotlin.idea.codeInliner

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelectorOrThis

class ClassUsageReplacementStrategy(
        typeReplacement: KtUserType?,
        constructorReplacement: CodeToInline?,
        project: Project
) : UsageReplacementStrategy {

    private val factory = KtPsiFactory(project)

    private val typeReplacement = typeReplacement?.takeIf { it.referenceExpression != null }
    private val typeReplacementQualifierAsExpression = typeReplacement?.qualifier?.let { factory.createExpression(it.text) }

    private val constructorReplacementStrategy = constructorReplacement?.let {
        CallableUsageReplacementStrategy(it, inlineSetter = false)
    }

    override fun createReplacer(usage: KtSimpleNameExpression): (() -> KtElement?)? {
        if (usage !is KtNameReferenceExpression) return null

        constructorReplacementStrategy?.createReplacer(usage)?.let { return it }

        val parent = usage.parent
        when (parent) {
            is KtUserType -> {
                if (typeReplacement == null) return null
                return {
                    val replaced = parent.replaced(typeReplacement)
                    ShortenReferences.DEFAULT.process(replaced)
                } //TODO: type arguments and type arguments of outer class are lost
            }

            is KtCallExpression -> {
                if (usage != parent.calleeExpression) return null
                when {
                    constructorReplacementStrategy == null && typeReplacement != null -> return {
                        replaceConstructorCallWithOtherTypeConstruction(parent)
                    }
                    else -> return null
                }
            }

            else -> {
                if (typeReplacement != null) {
                    val fqNameStr = typeReplacement.text
                    val fqName = FqName(fqNameStr)

                    return {
                        usage.mainReference.bindToFqName(fqName, KtSimpleNameReference.ShorteningMode.FORCED_SHORTENING) as? KtElement
                    }
                }

                return null
            }
        }
    }

    private fun replaceConstructorCallWithOtherTypeConstruction(callExpression: KtCallExpression): KtElement {
        callExpression.calleeExpression!!.replace(typeReplacement!!.referenceExpression!!)

        val expressionToReplace = callExpression.getQualifiedExpressionForSelectorOrThis()
        val newExpression = if (typeReplacementQualifierAsExpression != null)
            factory.createExpressionByPattern("$0.$1", typeReplacementQualifierAsExpression, callExpression)
        else
            callExpression

        val result = if (expressionToReplace != newExpression) {
            expressionToReplace.replaced(newExpression)
        }
        else {
            expressionToReplace
        }

        return ShortenReferences.DEFAULT.process(result)
    }
}