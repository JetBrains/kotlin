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
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFully
import org.jetbrains.kotlin.idea.conversion.copy.range
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall

class RemoveUnnecessaryLateinitInspection : IntentionBasedInspection<KtProperty>(RemoveUnnecessaryLateinitIntention()) {
    override val problemHighlightType = ProblemHighlightType.LIKE_UNUSED_SYMBOL

    override val problemText = "Unnecessary lateinit"
}

class RemoveUnnecessaryLateinitIntention : SelfTargetingRangeIntention<KtProperty>(KtProperty::class.java, "Remove unnecessary lateinit") {
    override fun applicabilityRange(element: KtProperty): TextRange? {
        if (!element.hasModifier(KtTokens.LATEINIT_KEYWORD)) return null
        val ktClass = element.getStrictParentOfType<KtClass>() ?: return null
        if (ktClass.getAnonymousInitializers().any { hasAssignmentStatements(it.body, element) }) {
            return element.lateinitTextRange()
        }
        if (ktClass.hasPrimaryConstructor()) return null
        val secondaryConstructors = ktClass.getSecondaryConstructors()

        val secondaryConstructorsInfo = secondaryConstructors.map { secondaryConstructor ->
            val delegationCall = secondaryConstructor.getDelegationCall()
            val hasAssignmentStatements = hasAssignmentStatements(secondaryConstructor.bodyExpression, element)
            if (!hasAssignmentStatements && !delegationCall.isCallToThis) return null
            val resolvedCall = delegationCall.getResolvedCall(delegationCall.analyzeFully()) ?: return null
            val delegationCallIndex = secondaryConstructors.indexOfFirst {
                DescriptorToSourceUtils.descriptorToDeclaration(resolvedCall.resultingDescriptor) == it
            }
            SecondaryConstructorInfo(hasAssignmentStatements, delegationCallIndex)
        }
        return if (assignmentReachableFromAnySecondaryConstructor(secondaryConstructorsInfo)) element.lateinitTextRange()
        else null
    }

    private fun assignmentReachableFromAnySecondaryConstructor(constructorsInfo: List<SecondaryConstructorInfo>): Boolean {
        constructorsInfo.forEach {
            val visitedInfo = hashSetOf<SecondaryConstructorInfo>()
            var currentConstructor = it
            while (!currentConstructor.hasAssignmentStatement) {
                if (!visitedInfo.add(currentConstructor)) return false
                currentConstructor = constructorsInfo.getOrNull(currentConstructor.delegationCallIndex) ?: return false
            }
        }
        return true
    }

    private fun KtProperty.lateinitTextRange() = modifierList?.getModifier(KtTokens.LATEINIT_KEYWORD)?.range

    private fun hasAssignmentStatements(body: KtExpression?, property: KtProperty) =
            body?.children?.any {
                it is KtBinaryExpression &&
                it.operationToken === KtTokens.EQ &&
                it.left?.text == property.name
            } ?: false

    override fun applyTo(element: KtProperty, editor: Editor?) {
        element.removeModifier(KtTokens.LATEINIT_KEYWORD)
    }
}

private data class SecondaryConstructorInfo(val hasAssignmentStatement: Boolean, val delegationCallIndex: Int)