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

import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.refactoring.addTypeArgumentsIfNeeded
import org.jetbrains.kotlin.idea.refactoring.getQualifiedTypeArgumentList
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.types.typeUtil.isUnit

class RemoveExplicitTypeIntention : SelfTargetingRangeIntention<KtCallableDeclaration>(
    KtCallableDeclaration::class.java,
    "Remove explicit type specification"
), HighPriorityAction {

    override fun applicabilityRange(element: KtCallableDeclaration): TextRange? {
        return getRange(element)
    }

    override fun applyTo(element: KtCallableDeclaration, editor: Editor?) {
        removeExplicitType(element)
    }

    companion object {
        fun removeExplicitType(element: KtCallableDeclaration) {
            val initializer = (element as? KtProperty)?.initializer
            val typeArgumentList = initializer?.let { getQualifiedTypeArgumentList(it) }
            element.typeReference = null
            if (typeArgumentList != null) addTypeArgumentsIfNeeded(initializer, typeArgumentList)
        }

        fun getRange(element: KtCallableDeclaration): TextRange? {
            if (element.containingFile is KtCodeFragment) return null
            val typeReference = element.typeReference ?: return null
            if (typeReference.annotationEntries.isNotEmpty()) return null

            if (element is KtParameter) {
                if (element.isLoopParameter) return element.textRange
                if (element.isSetterParameter) return typeReference.textRange
            }

            if (element !is KtProperty && element !is KtNamedFunction) return null

            if (element is KtNamedFunction
                && element.hasBlockBody()
                && (element.descriptor as? FunctionDescriptor)?.returnType?.isUnit()?.not() != false
            ) return null

            val initializer = (element as? KtDeclarationWithInitializer)?.initializer

            if (!redundantTypeSpecification(element.typeReference, initializer)) return null

            return when {
                initializer != null -> TextRange(element.startOffset, initializer.startOffset - 1)
                element is KtProperty && element.getter != null -> TextRange(element.startOffset, typeReference.endOffset)
                element is KtNamedFunction -> TextRange(element.startOffset, typeReference.endOffset)
                else -> null
            }
        }

        tailrec fun redundantTypeSpecification(typeReference: KtTypeReference?, initializer: KtExpression?): Boolean {
            if (initializer == null || typeReference == null) return true
            if (initializer !is KtLambdaExpression && initializer !is KtNamedFunction) return true
            val typeElement = typeReference.typeElement ?: return true
            return when (typeElement) {
                is KtFunctionType -> {
                    if (typeElement.receiver != null) return false
                    if (typeElement.parameters.isEmpty()) return true
                    val valueParameters = when (initializer) {
                        is KtLambdaExpression -> initializer.valueParameters
                        is KtNamedFunction -> initializer.valueParameters
                        else -> emptyList()
                    }
                    valueParameters.isNotEmpty() && valueParameters.none { it.typeReference == null }
                }
                is KtUserType -> {
                    val typeAlias = typeElement.referenceExpression?.mainReference?.resolve() as? KtTypeAlias ?: return true
                    return redundantTypeSpecification(typeAlias.getTypeReference(), initializer)
                }
                else -> true
            }
        }
    }
}

internal val KtParameter.isSetterParameter: Boolean get() = (parent.parent as? KtPropertyAccessor)?.isSetter ?: false