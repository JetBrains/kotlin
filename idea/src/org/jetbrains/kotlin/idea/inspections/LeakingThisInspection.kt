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

import com.intellij.codeInspection.IntentionWrapper
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType.GENERIC_ERROR_OR_WARNING
import com.intellij.codeInspection.ProblemHighlightType.WEAK_WARNING
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.search.searches.DefinitionsScopedSearch
import org.jetbrains.kotlin.cfg.LeakingThisDescriptor.*
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFully
import org.jetbrains.kotlin.idea.quickfix.AddModifierFix
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.resolve.BindingContext.LEAKING_THIS
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils

class LeakingThisInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitExpression(expression: KtExpression) {
                val context = expression.analyzeFully()
                val leakingThisDescriptor = context.get(LEAKING_THIS, expression) ?: return
                val description = when (leakingThisDescriptor) {
                    is NonFinalClass ->
                        if (expression is KtThisExpression)
                            "Leaking 'this' in constructor of non-final class ${leakingThisDescriptor.klass.name}"
                        else
                            return // Not supported yet
                    is NonFinalProperty ->
                        "Accessing non-final property ${leakingThisDescriptor.property.name} in constructor"
                    is NonFinalFunction ->
                        "Calling non-final function ${leakingThisDescriptor.function.name} in constructor"
                    else -> return // Not supported yet
                }
                val memberDescriptorToFix = when (leakingThisDescriptor) {
                    is NonFinalProperty -> leakingThisDescriptor.property
                    is NonFinalFunction -> leakingThisDescriptor.function
                    else -> null
                }
                val memberFix = memberDescriptorToFix?.let {
                    if (it.modality == Modality.OPEN) {
                        val modifierListOwner = DescriptorToSourceUtils.descriptorToDeclaration(it) as? KtDeclaration
                        createMakeFinalFix(modifierListOwner)
                    }
                    else null
                }

                val klass = leakingThisDescriptor.classOrObject as? KtClass
                val classFix =
                        if (klass != null && klass.hasModifier(KtTokens.OPEN_KEYWORD)) {
                            createMakeFinalFix(klass)
                        }
                        else null

                holder.registerProblem(
                        expression, description,
                        when (leakingThisDescriptor) {
                            is NonFinalProperty, is NonFinalFunction -> GENERIC_ERROR_OR_WARNING
                            else -> WEAK_WARNING
                        },
                        *(arrayOf(memberFix, classFix).filterNotNull().toTypedArray())
                )
            }
        }
    }


    companion object {
        private fun createMakeFinalFix(declaration: KtDeclaration?): IntentionWrapper? {
            declaration ?: return null
            val useScope = declaration.useScope
            if (DefinitionsScopedSearch.search(declaration, useScope).findFirst() != null) return null
            if ((declaration.containingClassOrObject as? KtClass)?.isInterface() ?: false) return null
            return IntentionWrapper(AddModifierFix(declaration, KtTokens.FINAL_KEYWORD), declaration.containingFile)
        }
    }
}