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

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.IntentionWrapper
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.*
import org.jetbrains.kotlin.asJava.elements.KtLightFieldImpl.KtLightFieldForDeclaration
import org.jetbrains.kotlin.idea.inspections.MayBeConstantInspection.Status.*
import org.jetbrains.kotlin.idea.quickfix.AddConstModifierFix
import org.jetbrains.kotlin.psi.KtProperty

class FakeJvmFieldConstantInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitAnnotationParameterList(list: PsiAnnotationParameterList) {
                super.visitAnnotationParameterList(list)

                for (attribute in list.attributes) {
                    val valueExpression = attribute.value as? PsiExpression ?: continue
                    checkExpression(valueExpression, holder)
                }
            }

            override fun visitSwitchLabelStatement(statement: PsiSwitchLabelStatement) {
                super.visitSwitchLabelStatement(statement)

                val valueExpression = statement.caseValue ?: return
                checkExpression(valueExpression, holder)
            }
        }
    }

    private fun checkExpression(valueExpression: PsiExpression, holder: ProblemsHolder) {
        val resolvedLightField = (valueExpression as? PsiReference)?.resolve() as? KtLightFieldForDeclaration ?: return
        val resolvedProperty = resolvedLightField.kotlinOrigin as? KtProperty ?: return
        with(MayBeConstantInspection) {
            if (resolvedProperty.annotationEntries.isEmpty()) return@with
            val resolvedPropertyStatus = resolvedProperty.getStatus()
            if (resolvedPropertyStatus == JVM_FIELD_MIGHT_BE_CONST ||
                resolvedPropertyStatus == JVM_FIELD_MIGHT_BE_CONST_NO_INITIALIZER ||
                resolvedPropertyStatus == JVM_FIELD_MIGHT_BE_CONST_ERRONEOUS
            ) {
                val fixes = mutableListOf<LocalQuickFix>()
                if (resolvedPropertyStatus == JVM_FIELD_MIGHT_BE_CONST) {
                    fixes += IntentionWrapper(AddConstModifierFix(resolvedProperty), resolvedProperty.containingFile)
                }
                holder.registerProblem(
                    valueExpression,
                    "Use of non-const Kotlin property as Java constant is incorrect. Will be forbidden in 1.4",
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                    *fixes.toTypedArray()
                )
            }
        }
    }
}