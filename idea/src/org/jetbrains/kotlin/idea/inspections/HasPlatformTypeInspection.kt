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
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.SpecifyTypeExplicitlyIntention
import org.jetbrains.kotlin.idea.quickfix.AddExclExclCallFix
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNextSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.isFlexible
import org.jetbrains.kotlin.types.isNullabilityFlexible

class HasPlatformTypeInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitNamedDeclaration(declaration: KtNamedDeclaration) {
                super.visitDeclaration(declaration)

                val declType = when (declaration) {
                    is KtFunction -> "Function"
                    is KtProperty -> if (declaration.isLocal) "Variable" else "Property"
                    else -> return
                }

                val context = declaration.analyze(BodyResolveMode.PARTIAL)
                val returnType = declaration.getReturnType(context) ?: return
                if (!returnType.isFlexibleRecursive()) return

                val fixes = mutableListOf(IntentionWrapper(SpecifyTypeExplicitlyIntention(), declaration.containingFile))

                if (returnType.isNullabilityFlexible()) {
                    val expression = declaration.node.findChildByType(KtTokens.EQ)?.psi?.getNextSiblingIgnoringWhitespaceAndComments()
                    if (expression != null) {
                        fixes += IntentionWrapper(AddExclExclCallFix(expression), declaration.containingFile)
                    }
                }

                val nameElement = declaration.nameIdentifier ?: return
                val problemDescriptor = holder.manager.createProblemDescriptor(
                        nameElement,
                        nameElement,
                        "$declType has platform type. Make the type explicit to prevent subtle bugs.",
                        ProblemHighlightType.WEAK_WARNING,
                        isOnTheFly,
                        *fixes.toTypedArray()
                )
                holder.registerProblem(problemDescriptor)
            }

            private fun KtDeclaration.getReturnType(bindingContext: BindingContext): KotlinType? {
                val callable = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, this] as? CallableDescriptor ?: return null
                return callable.returnType
            }

            private fun KotlinType.isFlexibleRecursive(): Boolean {
                if (isFlexible()) return true
                return arguments.any { it.type.isFlexibleRecursive() }
            }
        }
    }


}