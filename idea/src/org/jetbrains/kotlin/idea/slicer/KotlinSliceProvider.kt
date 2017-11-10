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

package org.jetbrains.kotlin.idea.slicer

import com.intellij.codeInspection.dataFlow.Nullness
import com.intellij.ide.util.treeView.AbstractTreeStructure
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.psi.PsiElement
import com.intellij.slicer.*
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyzeAndGetResult
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.guessTypes
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.isPlainWithEscapes
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.isError
import org.jetbrains.kotlin.types.isNullabilityFlexible

class KotlinSliceProvider : SliceLanguageSupportProvider {
    companion object {
        val LEAF_ELEMENT_EQUALITY = object : SliceLeafEquality() {
            override fun substituteElement(element: PsiElement) = (element as? KtReference)?.resolve() ?: element
        }
    }

    class KotlinGroupByNullnessAction(treeBuilder: SliceTreeBuilder) : GroupByNullnessActionBase(treeBuilder) {
        override fun isAvailable() = true
    }

    val leafAnalyzer by lazy { SliceLeafAnalyzer(LEAF_ELEMENT_EQUALITY, this) }
    val nullnessAnalyzer: SliceNullnessAnalyzerBase by lazy {
        object : SliceNullnessAnalyzerBase(LEAF_ELEMENT_EQUALITY, this) {
            override fun checkNullness(element: PsiElement?): Nullness {
                val types = when (element) {
                    is KtCallableDeclaration -> listOfNotNull((element.resolveToDescriptorIfAny() as? CallableDescriptor)?.returnType)
                    is KtDeclaration -> emptyList()
                    is KtExpression -> {
                        val (bindingContext, moduleDescriptor) = element.analyzeAndGetResult()
                        element.guessTypes(bindingContext, moduleDescriptor).toList()
                    }
                    else -> emptyList()
                }
                return when {
                    types.isEmpty() -> return Nullness.UNKNOWN
                    types.all { KotlinBuiltIns.isNullableNothing(it) } -> Nullness.NULLABLE
                    types.any { it.isError || TypeUtils.isNullableType(it) || it.isNullabilityFlexible() } -> Nullness.UNKNOWN
                    else -> Nullness.NOT_NULL
                }
            }
        }
    }

    override fun createRootUsage(element: PsiElement, params: SliceAnalysisParams) = KotlinSliceUsage(element, params)

    override fun getExpressionAtCaret(atCaret: PsiElement?, dataFlowToThis: Boolean): KtExpression? {
        val element =
                atCaret?.parentsWithSelf
                        ?.firstOrNull {
                            it is KtProperty ||
                            it is KtParameter ||
                            it is KtDeclarationWithBody ||
                            (it is KtExpression && it !is KtDeclaration)
                        }
                        ?.let { KtPsiUtil.safeDeparenthesize(it as KtExpression) } ?: return null
        if (dataFlowToThis) {
            if (element is KtConstantExpression) return null
            if (element is KtStringTemplateExpression && element.isPlainWithEscapes()) return null
            if (element is KtClassLiteralExpression) return null
            if (element is KtCallableReferenceExpression) return null
        }
        return element
    }

    override fun getElementForDescription(element: PsiElement): PsiElement {
        return (element as? KtSimpleNameExpression)?.mainReference?.resolve() ?: element
    }

    override fun getRenderer() = KotlinSliceUsageCellRenderer

    override fun startAnalyzeLeafValues(structure: AbstractTreeStructure, finalRunnable: Runnable) {
        leafAnalyzer.startAnalyzeValues(structure, finalRunnable)
    }

    override fun startAnalyzeNullness(structure: AbstractTreeStructure, finalRunnable: Runnable) {
        nullnessAnalyzer.startAnalyzeNullness(structure, finalRunnable)
    }

    override fun registerExtraPanelActions(group: DefaultActionGroup, builder: SliceTreeBuilder) {
        if (builder.dataFlowToThis) {
            group.add(GroupByLeavesAction(builder))
            group.add(KotlinGroupByNullnessAction(builder))
        }
    }
}