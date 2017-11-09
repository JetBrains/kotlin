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

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.psiUtil.isPrivate

class DataClassPrivateConstructorInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitPrimaryConstructor(constructor: KtPrimaryConstructor) {
                super.visitPrimaryConstructor(constructor)

                if (constructor.containingClass()?.isData() == true && constructor.isPrivate()) {
                    val keyword = constructor.modifierList?.getModifier(KtTokens.PRIVATE_KEYWORD) ?: return
                    val problemDescriptor = holder.manager.createProblemDescriptor(
                            keyword,
                            keyword,
                            "Private data class constructor is exposed via the generated 'copy' method.",
                            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                            isOnTheFly
                    )

                    holder.registerProblem(problemDescriptor)
                }
            }
        }
    }
}