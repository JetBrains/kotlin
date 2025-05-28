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

import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.psi.stubs.elements.KtTokenSets

open class KtFile(viewProvider: FileViewProvider, isCompiled: Boolean) : @Suppress("DEPRECATION") KtCommonFile(viewProvider, isCompiled),
    PsiClassOwner {
    override fun getClasses(): Array<PsiClass> {
        val fileClassProvider = project.getService(KtFileClassProvider::class.java)
        return fileClassProvider?.getFileClasses(this) ?: PsiClass.EMPTY_ARRAY
    }

    override fun setPackageName(packageName: String) {}

    @Deprecated("getPackageFqName should be used instead")
    override fun getPackageName(): String {
        return packageFqName.asString()
    }

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R {
        return visitor.visitKtFile(this, data)
    }

    companion object {
        val FILE_DECLARATION_TYPES = TokenSet.orSet(KtTokenSets.DECLARATION_TYPES, TokenSet.create(KtStubElementTypes.SCRIPT))
    }
}

