/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.stubs

import com.intellij.psi.stubs.PsiFileStub
import org.jetbrains.kotlin.analysis.decompiler.psi.file.KtDecompiledFile
import org.jetbrains.kotlin.analysis.utils.errors.requireIsInstance
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.psi.KtFile


/**
 * This test is supposed to validate the consistency between a stub-tree and an AST-tree for a [decompiled file][org.jetbrains.kotlin.analysis.decompiler.psi.file.KtDecompiledFile].
 *
 * Via [PsiFileImpl.calcStubTree][com.intellij.psi.impl.source.PsiFileImpl.calcStubTree] it performs:
 * 1. Builds the AST-tree from the decompiled text
 * 2. Builds the stub-tree from the binary data (effectively the same as [AbstractCompiledStubsTest])
 * 3. Binds the AST and stub trees (via [com.intellij.psi.impl.source.FileTrees.reconcilePsi])
 *
 * @see org.jetbrains.kotlin.analysis.decompiler.psi.AbstractDecompiledTextTest
 */
abstract class AbstractDecompiledStubsTest(defaultTargetPlatform: TargetPlatform) : AbstractCompiledStubsTest(defaultTargetPlatform) {
    override fun computeStub(file: KtFile): PsiFileStub<*> {
        requireIsInstance<KtDecompiledFile>(file)

        return file.calcStubTree().root
    }
}
