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
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.intentions.RemoveExplicitTypeIntention
import org.jetbrains.kotlin.idea.intentions.isSetterParameter
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtVisitorVoid

class RemoveSetterParameterTypeInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitDeclaration(dcl: KtDeclaration) {
                val typeReference = (dcl as? KtParameter)?.takeIf { it.isSetterParameter }?.typeReference ?: return
                holder.registerProblem(typeReference,
                                       "Redundant setter parameter type",
                                       ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                                       IntentionWrapper(RemoveExplicitTypeIntention(), dcl.containingKtFile))
            }
        }
    }
}