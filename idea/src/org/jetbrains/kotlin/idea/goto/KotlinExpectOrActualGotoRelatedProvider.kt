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

package org.jetbrains.kotlin.idea.goto

import com.intellij.navigation.GotoRelatedItem
import com.intellij.navigation.GotoRelatedProvider
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.util.*
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypeAndBranch

class KotlinExpectOrActualGotoRelatedProvider : GotoRelatedProvider() {
    private class ActualOrExpectGotoRelatedItem(element: PsiElement): GotoRelatedItem(element) {
        override fun getCustomContainerName(): String? {
            val module = element?.module ?: return null
            return "(in module ${module.name})"
        }
    }

    override fun getItems(psiElement: PsiElement): List<GotoRelatedItem> {
        val declaration = psiElement.getParentOfTypeAndBranch<KtNamedDeclaration> { nameIdentifier } ?: return emptyList()
        val targets = when {
            declaration.isExpectDeclaration() -> declaration.actualsForExpected()
            declaration.isEffectivelyActual() -> listOfNotNull(declaration.expectedDeclarationIfAny())
            else -> emptyList()
        }
        return targets.map(::ActualOrExpectGotoRelatedItem)
    }
}