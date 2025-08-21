/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.psi.PsiComment
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
        val errorMessage = findErrorMessage(file)

        val kind = when {
            errorMessage != null -> KotlinFileStubKindImpl.Invalid(errorMessage)
            file.isScript() -> KotlinFileStubKindImpl.Script(packageFqName = packageFqName)
            file.hasTopLevelCallables() && (!file.isCompiled || file.name.endsWith(".class")) -> {
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

/**
 * Searches for a special error message from [KotlinFileStubKindImpl.Invalid].
 *
 * For now this place is aligned only with the decompiler.
 */
private fun findErrorMessage(file: KtFile): String? {
    if (!file.isCompiled) return null
    val firstComment = file.importList?.nextSibling as? PsiComment ?: return null
    if (firstComment.textMatches("// This file was compiled with a newer version of Kotlin compiler and can't be decompiled.")) {
        return file.text
    }

    return null
}
