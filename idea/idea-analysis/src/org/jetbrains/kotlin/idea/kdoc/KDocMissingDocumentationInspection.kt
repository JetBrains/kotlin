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

package org.jetbrains.kotlin.idea.kdoc

import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifierType
import org.jetbrains.kotlin.resolve.OverridingUtil

fun KtDeclaration.implicitVisibility(): KtModifierKeywordToken? {
    val defaultVisibilityKeyword = if (hasModifier(KtTokens.OVERRIDE_KEYWORD)) {
        (resolveToDescriptor() as? CallableMemberDescriptor)
                ?.overriddenDescriptors
                ?.let { OverridingUtil.findMaxVisibility(it) }
                ?.toKeywordToken()
    }
    else {
        KtTokens.DEFAULT_VISIBILITY_KEYWORD
    }
    return defaultVisibilityKeyword
}

fun Visibility.toKeywordToken(): KtModifierKeywordToken {
    val normalized = normalize()
    when (normalized) {
        Visibilities.PUBLIC -> return KtTokens.PUBLIC_KEYWORD
        Visibilities.PROTECTED -> return KtTokens.PROTECTED_KEYWORD
        Visibilities.INTERNAL -> return KtTokens.INTERNAL_KEYWORD
        else -> {
            if (Visibilities.isPrivate(normalized)) {
                return KtTokens.PRIVATE_KEYWORD
            }
            error("Unexpected visibility '$normalized'")
        }
    }
}


class KDocMissingDocumentationInspection(): AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
            KDocMissingDocumentationInspection(holder)

    private class KDocMissingDocumentationInspection(private val holder: ProblemsHolder): PsiElementVisitor() {
        override fun visitElement(element: PsiElement) {
            if (element is KtNamedDeclaration &&
                !element.hasModifier(KtTokens.OVERRIDE_KEYWORD) && element.visibilityModifierType() ?: element.implicitVisibility() == KtTokens.PUBLIC_KEYWORD &&
                element.docComment == null) {
                element.nameIdentifier?.let { holder.registerProblem(it, "Missing Documentation") }
            }
        }
    }
}
