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
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.extensions.DeclarationAttributeAltererExtension
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.implicitModality
import org.jetbrains.kotlin.idea.quickfix.RemoveModifierFix
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.modalityModifier
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class RedundantModalityModifierInspection : AbstractKotlinInspection(), CleanupLocalInspectionTool {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitDeclaration(declaration: KtDeclaration) {
                val modalityModifier = declaration.modalityModifier() ?: return
                val modalityModifierType = modalityModifier.node.elementType
                val implicitModality = declaration.implicitModality()

                if (modalityModifierType != implicitModality) return

                // Descriptor may have the different modality (in case of all-open plugin enabled, for example)
                val modalityFromPsi = getModality(implicitModality)
                if (modalityFromPsi != null && hasRefinedModalityInDescriptor(declaration, modalityFromPsi)) return

                holder.registerProblem(modalityModifier,
                                       "Redundant modality modifier",
                                       ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                                       IntentionWrapper(RemoveModifierFix(declaration, implicitModality, isRedundant = true),
                                                        declaration.containingFile))
            }
        }
    }

    private fun hasRefinedModalityInDescriptor(
            declaration: KtDeclaration,
            implicitModality: Modality
    ): Boolean {
        val bindingContext = declaration.analyze(BodyResolveMode.PARTIAL)
        val descriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, declaration] ?: return false
        return DeclarationAttributeAltererExtension.getInstances(declaration.project).any {
            it.refineDeclarationModality(declaration, descriptor, descriptor.containingDeclaration,
                                         implicitModality, bindingContext) != null
        }
    }

    private fun getModality(modalityToken: IElementType): Modality? = when (modalityToken) {
        KtTokens.FINAL_KEYWORD -> Modality.FINAL
        KtTokens.SEALED_KEYWORD -> Modality.SEALED
        KtTokens.OPEN_KEYWORD -> Modality.OPEN
        KtTokens.ABSTRACT_KEYWORD -> Modality.ABSTRACT
        else -> null
    }
}
