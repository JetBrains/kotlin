/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.psi.PsiFile
import com.intellij.psi.stubs.DefaultStubBuilder
import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.psiUtil.JvmFileClassUtil
import org.jetbrains.kotlin.psi.stubs.impl.KotlinFileStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.KotlinFileStubKindImpl

class KtFileStubBuilder : DefaultStubBuilder() {
    @OptIn(KtImplementationDetail::class)
    override fun createStubForFile(file: PsiFile): StubElement<*> {
        if (file !is KtFile) {
            return super.createStubForFile(file)
        }

        val packageFqName = file.packageFqName

        val kind = when {
            file.isScript() -> KotlinFileStubKindImpl.Script(packageFqName = packageFqName)
            file.hasTopLevelCallables() -> {
                val fileClassInfo = JvmFileClassUtil.getFileClassInfoNoResolve(file)
                KotlinFileStubKindImpl.Facade(
                    packageFqName = packageFqName,
                    facadeFqName = fileClassInfo.facadeClassFqName,
                )
            }

            else -> KotlinFileStubKindImpl.File(packageFqName = packageFqName)
        }

        return KotlinFileStubImpl(file, kind)
    }
}
