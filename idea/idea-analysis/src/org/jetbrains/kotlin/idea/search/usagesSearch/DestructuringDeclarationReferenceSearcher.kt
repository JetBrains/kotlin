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

package org.jetbrains.kotlin.idea.search.usagesSearch

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.SearchRequestCollector
import com.intellij.psi.search.SearchScope
import com.intellij.util.Processor
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.references.KtDestructuringDeclarationReference
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance

class DestructuringDeclarationReferenceSearcher(
        targetDeclaration: PsiElement,
        private val componentIndex: Int,
        searchScope: SearchScope,
        consumer: Processor<PsiReference>,
        optimizer: SearchRequestCollector
) : OperatorReferenceSearcher<KtDestructuringDeclaration>(targetDeclaration, searchScope, consumer, optimizer, wordsToSearch = listOf("(")) {

    override fun resolveTargetToDescriptor(): FunctionDescriptor? {
        if (targetDeclaration is KtParameter) {
            return targetDeclaration.dataClassComponentFunction()
        }
        else {
            return super.resolveTargetToDescriptor()
        }
    }

    override fun extractReference(element: KtElement): PsiReference? {
        val destructuringDeclaration = element as? KtDestructuringDeclaration ?: return null
        val entries = destructuringDeclaration.entries
        if (entries.size < componentIndex) return null
        return entries[componentIndex - 1].references.firstIsInstance<KtDestructuringDeclarationReference>()
    }

    override fun isReferenceToCheck(ref: PsiReference) = ref is KtDestructuringDeclarationReference

    override fun processPossibleReceiverExpression(expression: KtExpression) {
        val parent = expression.parent
        val destructuringDeclaration = when (parent) {
            is KtDestructuringDeclaration -> parent

            is KtContainerNode -> {
                if (parent.node.elementType == KtNodeTypes.LOOP_RANGE) {
                    (parent.parent as KtForExpression).destructuringParameter
                }
                else {
                    null
                }
            }

            else -> null
        }

        if (destructuringDeclaration != null) {
            processReferenceElement(destructuringDeclaration)
        }
    }
}