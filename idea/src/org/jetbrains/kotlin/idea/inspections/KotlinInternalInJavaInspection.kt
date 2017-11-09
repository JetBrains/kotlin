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

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.*
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.idea.caches.resolve.getNullableModuleInfo
import org.jetbrains.kotlin.lexer.KtTokens.INTERNAL_KEYWORD
import org.jetbrains.kotlin.psi.KtModifierListOwner


class KotlinInternalInJavaInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitReferenceExpression(expression: PsiReferenceExpression?) {
                expression?.checkAndReport(holder)
            }

            override fun visitReferenceElement(reference: PsiJavaCodeReferenceElement?) {
                reference?.checkAndReport(holder)
            }
        }
    }

    private fun PsiElement.checkAndReport(holder: ProblemsHolder) {
        val lightElement = (this as? PsiReference)?.resolve() as? KtLightElement<*, *> ?: return
        val modifierListOwner = lightElement.kotlinOrigin as? KtModifierListOwner ?: return
        if(inSameModule(modifierListOwner)) {
            return
        }

        if(modifierListOwner.hasModifier(INTERNAL_KEYWORD)) {
            holder.registerProblem(this, "Usage of Kotlin internal declaration from different module")
        }
    }

    private fun PsiElement.inSameModule(element: PsiElement) = getNullableModuleInfo()?.equals(element.getNullableModuleInfo()) ?: true
}
