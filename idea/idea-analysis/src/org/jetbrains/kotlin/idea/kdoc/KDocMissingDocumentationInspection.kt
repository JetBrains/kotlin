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
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.psiUtil.isPrivate

class KDocMissingDocumentationInspection(): AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
            KDocMissingDocumentationInspection(holder)

    private class KDocMissingDocumentationInspection(private val holder: ProblemsHolder): PsiElementVisitor() {
        override fun visitElement(element: PsiElement) {
            if (element is KtNamedDeclaration && !element.isPrivate() && element.docComment == null) {
                val nameIdentifier = element.nameIdentifier
                if (nameIdentifier != null) holder.registerProblem(nameIdentifier, "Missing Documentation")
            }
        }
    }
}
