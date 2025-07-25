/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.psi.PsiFile
import com.intellij.psi.stubs.DefaultStubBuilder
import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.JvmFileClassUtil
import org.jetbrains.kotlin.psi.stubs.impl.KotlinFileStubImpl

class KtFileStubBuilder : DefaultStubBuilder() {
    override fun createStubForFile(file: PsiFile): StubElement<*> {
        if (file !is KtFile) {
            return super.createStubForFile(file)
        }

        val packageFqName = file.packageFqName.asString()
        val isScript = file.isScript()
        if (file.hasTopLevelCallables()) {
            val fileClassInfo = JvmFileClassUtil.getFileClassInfoNoResolve(file)
            val facadeFqNameRef = fileClassInfo.facadeClassFqName.asString()
            val partSimpleName = fileClassInfo.fileClassFqName.shortName().asString()
            return KotlinFileStubImpl(
                ktFile = file,
                packageName = packageFqName,
                isScript = isScript,
                facadeFqNameString = facadeFqNameRef,
                partSimpleName = partSimpleName,
                facadePartSimpleNames = null,
            )
        }

        return KotlinFileStubImpl(
            ktFile = file,
            packageName = packageFqName,
            isScript = isScript,
            facadeFqNameString = null,
            partSimpleName = null,
            facadePartSimpleNames = null,
        )
    }
}
