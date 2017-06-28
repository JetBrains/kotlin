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

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.codeInsight.daemon.RainbowVisitor
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.highlighter.KotlinHighlightingColors.*
import org.jetbrains.kotlin.kdoc.psi.impl.KDocName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.bindingContextUtil.getReferenceTargets
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

class KotlinRainbowVisitor : RainbowVisitor() {
    override fun suitableForFile(file: PsiFile) = file is KtFile

    override fun clone() = KotlinRainbowVisitor()

    override fun visit(element: PsiElement) {
        when {
            element.isRainbowDeclaration() -> {
                val rainbowElement = (element as KtNamedDeclaration).nameIdentifier ?: return
                addRainbowHighlight(element, rainbowElement)
            }
            element is KtSimpleNameExpression -> {
                val qualifiedExpression = PsiTreeUtil.getParentOfType(element, KtQualifiedExpression::class.java, true,
                                                                      KtLambdaExpression::class.java, KtValueArgumentList::class.java)
                if (qualifiedExpression?.selectorExpression?.isAncestor(element) == true) return

                val bindingContext = element.analyze(BodyResolveMode.PARTIAL_WITH_DIAGNOSTICS)
                val targets = element.getReferenceTargets(bindingContext)
                val targetVariable = targets.firstIsInstanceOrNull<VariableDescriptor>()
                if (targetVariable != null) {
                    val targetElement = DescriptorToSourceUtils.getSourceFromDescriptor(targetVariable)
                    if (targetElement.isRainbowDeclaration()) {
                        addRainbowHighlight(targetElement!!, element)
                    }
                    else if (targetElement == null && element.getReferencedName() == "it") {
                        addRainbowHighlight(element, element)
                    }
                }
            }
            element is KDocName -> {
                val target = element.reference?.resolve() ?: return
                if (target.isRainbowDeclaration()) {
                    addRainbowHighlight(target, element, KDOC_LINK)
                }
            }
        }
    }

    private fun addRainbowHighlight(target: PsiElement, rainbowElement: PsiElement,
                                    attributesKey: TextAttributesKey? = null) {
        val lambdaSequenceIterator = target.parents
                .takeWhile { it !is KtDeclaration || it.isAnonymousFunction() || it is KtFunctionLiteral }
                .filter { it is KtLambdaExpression || it.isAnonymousFunction() }
                .iterator()

       val attributesKeyToUse = attributesKey ?: (if (target is KtParameter) PARAMETER else LOCAL_VARIABLE)
        if (lambdaSequenceIterator.hasNext()) {
            var lambda = lambdaSequenceIterator.next()
            var lambdaNestingLevel = 0
            while (lambdaSequenceIterator.hasNext()) {
                lambdaNestingLevel++
                lambda = lambdaSequenceIterator.next()
            }
            addInfo(getInfo(lambda, rainbowElement, "$lambdaNestingLevel${rainbowElement.text}", attributesKeyToUse))
            return
        }

        val context = target.getStrictParentOfType<KtDeclarationWithBody>() ?: return
        addInfo(getInfo(context, rainbowElement, rainbowElement.text, attributesKeyToUse))
    }

    private fun PsiElement?.isRainbowDeclaration(): Boolean =
            (this is KtProperty && isLocal) ||
            (this is KtParameter && getStrictParentOfType<KtPrimaryConstructor>() == null) ||
            this is KtDestructuringDeclarationEntry
}

private fun PsiElement.isAnonymousFunction(): Boolean = this is KtNamedFunction && nameIdentifier == null
