/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.psi.file

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiFile
import com.intellij.psi.StubBuilder
import com.intellij.psi.stubs.StubTreeLoader
import org.jetbrains.kotlin.analysis.decompiler.psi.KotlinDecompiledFileViewProvider
import org.jetbrains.kotlin.analysis.decompiler.psi.text.buildDecompiledText
import org.jetbrains.kotlin.analysis.decompiler.stub.file.ClsClassFinder
import org.jetbrains.kotlin.analysis.utils.errors.requireIsInstance
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.stubs.impl.KotlinFileStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.deepCopy
import org.jetbrains.kotlin.utils.concurrent.block.LockedClearableLazyValue

abstract class KtDecompiledFile(private val provider: KotlinDecompiledFileViewProvider) : KtFile(provider, true) {
    @OptIn(KtImplementationDetail::class)
    override val customStubBuilder: StubBuilder?
        get() = CompiledStubBuilder

    private val decompiledText = LockedClearableLazyValue(Any()) {
        val stub = CompiledStubBuilder.readOrBuildCompiledStub(this)
        buildDecompiledText(stub)
    }

    override fun getText(): String? {
        return decompiledText.get()
    }

    override fun onContentReload() {
        super.onContentReload()

        provider.content.drop()
        decompiledText.drop()
    }
}

private object CompiledStubBuilder : StubBuilder {
    override fun buildStubTree(file: PsiFile): KotlinFileStubImpl {
        requireIsInstance<KtDecompiledFile>(file)
        val stub = readOrBuildCompiledStub(file)

        // A copy is required because stubs are stateful and mutable, so they cannot be shared as they are
        @OptIn(KtImplementationDetail::class)
        val clonedStub = stub.deepCopy()
        clonedStub.psi = file
        return clonedStub
    }

    fun readOrBuildCompiledStub(file: KtDecompiledFile): KotlinFileStubImpl {
        val virtualFile = file.viewProvider.virtualFile
        val project = file.project

        val stubTree = ClsClassFinder.allowMultifileClassPart {
            val stubLoader = StubTreeLoader.getInstance()

            // The default project is not supported in the stub loader
            if (project.isDefault) {
                stubLoader.build(/* project = */ null,/* vFile = */ virtualFile,/* psiFile = */ null)
            } else {
                // Read stub from cache if it is present
                stubLoader.readOrBuild(/* project = */ project,/* vFile = */ virtualFile,/* psiFile = */ null)
            }
        }

        val fileStub = stubTree?.root as? KotlinFileStubImpl
        return if (fileStub != null) {
            fileStub
        } else {
            val cause = if (stubTree == null) {
                "stub tree is not found"
            } else {
                "non-Kotlin stub tree (${stubTree::class.simpleName})"
            }

            val text = """
                // Could not decompile the file: $cause
                // Please report an issue: https://kotl.in/issue
            """.trimIndent()

            KotlinFileStubImpl.forInvalid(text)
        }
    }

    override fun skipChildProcessingWhenBuildingStubs(parent: ASTNode, node: ASTNode): Boolean = false
}
