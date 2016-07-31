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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFully
import org.jetbrains.kotlin.idea.conversion.copy.range
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCallWithAssert
import org.jetbrains.kotlin.resolve.source.getPsi

class RemoveUnnecessaryLateinitInspection : IntentionBasedInspection<KtProperty>(RemoveUnnecessaryLateinitIntention()) {
    override val problemHighlightType: ProblemHighlightType
        get() = ProblemHighlightType.LIKE_UNUSED_SYMBOL
}

class RemoveUnnecessaryLateinitIntention : SelfTargetingRangeIntention<KtProperty>(KtProperty::class.java, "Remove unnecessary lateinit") {
    override fun applicabilityRange(element: KtProperty): TextRange? {
        if (!element.hasModifier(KtTokens.LATEINIT_KEYWORD)) return null
        val ktClass = element.getNonStrictParentOfType<KtClass>() ?: return null
        val elementName = element.name ?: return null
        if (ktClass.getAnonymousInitializers().firstOrNull { hasAssignmentExpression(it.body?.children, elementName) } != null) return createTextRangeOfLateinit(element)
        if (ktClass.hasPrimaryConstructor()) return null
        val secondaryConstructors = ktClass.getSecondaryConstructors()
        val secondaryConstructorsInfo = secondaryConstructors.mapIndexed { index, secondaryConstructor ->
            val delegationCall = secondaryConstructor.getDelegationCall()
            val hasAssignmentExpression = hasAssignmentExpression(secondaryConstructor.bodyExpression?.children, elementName)
            if (!hasAssignmentExpression && !delegationCall.isCallToThis) return null
            val resolvedCall = delegationCall.getResolvedCallWithAssert(delegationCall.analyzeFully())
            val delegationCallIndex = secondaryConstructors.indexOfFirst { isSameKtParametersAndValueParameterDescriptor(it.valueParameters, resolvedCall.candidateDescriptor.valueParameters) }
            SecondaryConstructorInfo(index, hasAssignmentExpression, delegationCallIndex)
        }
        return if (hasAssignmentExpressionForSecondaryConstructors(secondaryConstructorsInfo)) createTextRangeOfLateinit(element) else null
    }

    private fun hasAssignmentExpressionForSecondaryConstructors(secondaryConstructorsInfo: List<SecondaryConstructorInfo>): Boolean {
        val isCalledIndex: MutableList<Int> = arrayListOf()
        secondaryConstructorsInfo.forEach {
            var currentSecondaryConstructor = it
            while (!currentSecondaryConstructor.hasAssignmentExpression) {
                if (isCalledIndex.contains(currentSecondaryConstructor.index)) return false
                isCalledIndex.add(currentSecondaryConstructor.index)
                currentSecondaryConstructor = secondaryConstructorsInfo.getOrNull(currentSecondaryConstructor.delegationCallIndex) ?: return false
            }
            isCalledIndex.clear()
        }
        return true
    }

    private fun createTextRangeOfLateinit(element: KtProperty): TextRange? {
        return element.modifierList?.getModifier(KtTokens.LATEINIT_KEYWORD)?.range
    }

    private fun isSameKtParametersAndValueParameterDescriptor(ktParameters: List<KtParameter>, valueParameterDescriptor: List<ValueParameterDescriptor>): Boolean {
        if (ktParameters.size != valueParameterDescriptor.size) return false
        ktParameters.forEachIndexed { index, ktParameter ->
            if (valueParameterDescriptor[index].source.getPsi() != ktParameter) return false
        }
        return true
    }

    private fun hasAssignmentExpression(elements: Array<PsiElement>?, variableName: String): Boolean {
        return elements?.firstOrNull {
            it is KtBinaryExpression &&
            it.operationToken == KtTokens.EQ &&
            it.left?.text == variableName
        } != null
    }

    override fun applyTo(element: KtProperty, editor: Editor?) {
        element.removeModifier(KtTokens.LATEINIT_KEYWORD)
    }
}

private data class SecondaryConstructorInfo(val index: Int, val hasAssignmentExpression: Boolean, val delegationCallIndex: Int)