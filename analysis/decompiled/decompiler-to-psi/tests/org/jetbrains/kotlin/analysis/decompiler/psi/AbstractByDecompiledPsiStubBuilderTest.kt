/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.psi

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.extensions.LoadingOrder
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.ClassFileViewProviderFactory
import com.intellij.psi.FileTypeFileViewProviders
import com.intellij.psi.PsiManager
import com.intellij.psi.compiled.ClassFileDecompilers
import com.intellij.psi.stubs.PsiFileStub
import org.jetbrains.kotlin.analysis.decompiler.stub.files.AbstractStubBuilderTest
import org.jetbrains.kotlin.psi.stubs.elements.KtFileStubBuilder

abstract class AbstractByDecompiledPsiStubBuilderTest : AbstractStubBuilderTest() {
    override fun setUp() {
        super.setUp()

        FileTypeFileViewProviders.INSTANCE.addExplicitExtension(JavaClassFileType.INSTANCE, ClassFileViewProviderFactory())

        ClassFileDecompilers.getInstance().EP_NAME.point.apply {
            registerExtension(KotlinClassFileDecompiler(), LoadingOrder.FIRST, testRootDisposable)
            registerExtension(KotlinBuiltInDecompiler(), LoadingOrder.FIRST, testRootDisposable)
        }

    }

    override fun getStubToTest(classFile: VirtualFile): PsiFileStub<*> {
        val decompiledFile = PsiManager.getInstance(project).findFile(classFile)
            ?: error("No decompiled file was found for $classFile")
        return KtFileStubBuilder().buildStubTree(decompiledFile) as PsiFileStub<*>
    }

    override fun skipBinaryStubOnlyTest(): Boolean = true
}