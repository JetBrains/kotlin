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

package org.jetbrains.kotlin.idea.refactoring.rename

import com.intellij.psi.PsiElement
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.psi.KtTypeParameter

class RenameKotlinTypeParameterProcessor : RenameKotlinPsiProcessor() {
    override fun canProcessElement(element: PsiElement) = element is KtTypeParameter

    override fun findCollisions(
            element: PsiElement,
            newName: String?,
            allRenames: MutableMap<out PsiElement, String>,
            result: MutableList<UsageInfo>
    ) {
        if (newName == null) return
        val declaration = element as? KtTypeParameter ?: return
        val descriptor = declaration.unsafeResolveToDescriptor()
        checkRedeclarations(descriptor, newName, result)
    }
}