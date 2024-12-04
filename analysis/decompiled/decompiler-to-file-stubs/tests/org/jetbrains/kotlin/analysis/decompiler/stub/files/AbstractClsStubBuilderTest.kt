/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.stub.files

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.util.indexing.FileContentImpl
import org.jetbrains.kotlin.analysis.decompiler.stub.file.KotlinClsStubBuilder

abstract class AbstractClsStubBuilderTest : AbstractStubBuilderTest() {
    override fun getStubToTest(classFile: VirtualFile): PsiFileStub<*> {
        return KotlinClsStubBuilder().buildFileStub(FileContentImpl.createByFile(classFile))!!
    }
}