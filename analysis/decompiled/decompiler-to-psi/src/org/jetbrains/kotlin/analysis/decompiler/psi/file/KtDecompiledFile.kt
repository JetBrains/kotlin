/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.psi.file

import com.intellij.lang.ASTNode
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.StubBuilder
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubTreeLoader
import org.jetbrains.kotlin.analysis.decompiler.psi.KotlinDecompiledFileViewProvider
import org.jetbrains.kotlin.analysis.decompiler.psi.text.DecompiledText
import org.jetbrains.kotlin.analysis.decompiler.psi.text.buildDecompiledText
import org.jetbrains.kotlin.analysis.decompiler.stub.file.ClsClassFinder
import org.jetbrains.kotlin.analysis.utils.errors.requireIsInstance
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.stubs.KotlinStubElement
import org.jetbrains.kotlin.psi.stubs.impl.KotlinFileStubImpl
import org.jetbrains.kotlin.utils.concurrent.block.LockedClearableLazyValue
import org.jetbrains.kotlin.utils.exceptions.checkWithAttachment

open class KtDecompiledFile(
    private val provider: KotlinDecompiledFileViewProvider,
    buildDecompiledText: (VirtualFile) -> DecompiledText,
) : KtFile(provider, true) {
    @OptIn(KtImplementationDetail::class)
    override val customStubBuilder: StubBuilder?
        get() = if (stubBasedDecompilerEnabled) {
            CompiledStubBuilder
        } else {
            null
        }

    private val decompiledText = LockedClearableLazyValue(Any()) {
        if (stubBasedDecompilerEnabled) {
            val stub = CompiledStubBuilder.readOrBuildCompiledStub(this)
            buildDecompiledText(stub)
        } else {
            buildDecompiledText(provider.virtualFile).text
        }
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

private val stubBasedDecompilerEnabled: Boolean by lazyPub {
    Registry.`is`("kotlin.analysis.stub.based.decompiler", true)
}

/** Creates a deep copy of the given [this] */
@KtImplementationDetail
fun KotlinFileStubImpl.deepCopy(): KotlinFileStubImpl = copyStubRecursively(
    originalStub = this,
    newParentStub = null,
) as KotlinFileStubImpl

/**
 * Returns a copy of [originalStub].
 */
@OptIn(KtImplementationDetail::class)
private fun <T : PsiElement> copyStubRecursively(
    originalStub: StubElement<T>,
    newParentStub: StubElement<*>?,
): StubElement<*> {
    requireIsInstance<KotlinStubElement<*>>(originalStub)
    val stubCopy = originalStub.copyInto(newParentStub)
    checkWithAttachment(
        originalStub::class == stubCopy::class,
        { "${originalStub::class.simpleName} is expected, but ${stubCopy::class.simpleName} is found" },
    )

    for (originalChild in originalStub.childrenStubs) {
        copyStubRecursively(originalStub = originalChild, newParentStub = stubCopy)
    }

    return stubCopy
}
