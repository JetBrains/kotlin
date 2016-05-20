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

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.*
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.core.implicitVisibility
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifier

class RedundantVisibilityModifierInspection : AbstractKotlinInspection(), CleanupLocalInspectionTool {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitDeclaration(declaration: KtDeclaration) {
                val visibilityModifier = declaration.visibilityModifier() ?: return
                if (visibilityModifier.node.elementType == declaration.implicitVisibility()) {
                    holder.registerProblem(visibilityModifier,
                                           "Redundant visibility modifier",
                                           ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                                           RemoveModifierFix("Remove redundant visibility modifier"))
                }
            }
        }
    }
}
