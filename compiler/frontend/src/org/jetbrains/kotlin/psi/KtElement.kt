/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.psi

import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiReference

interface KtElement : NavigatablePsiElement {
    fun getContainingKtFile(): KtFile

    fun <D> acceptChildren(visitor: KtVisitor<Void, D>, data: D)

    fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R

    @Deprecated("Don't use getReference() on JetElement for the choice is unpredictable")
    override fun getReference(): PsiReference?
}

fun KtElement.getModificationStamp(): Long {
    return when (this) {
        is KtFile -> this.modificationStamp
        is KtDeclarationStub<*> -> this.modificationStamp
        is KtSuperTypeList -> this.modificationStamp
        else -> (parent as KtElement).getModificationStamp()
    }
}
