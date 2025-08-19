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
import com.intellij.psi.stubs.*
import com.intellij.util.io.AbstractStringEnumerator
import com.intellij.util.io.StringRef
import com.intellij.util.io.UnsyncByteArrayOutputStream
import org.jetbrains.kotlin.analysis.decompiler.psi.KotlinDecompiledFileViewProvider
import org.jetbrains.kotlin.analysis.decompiler.psi.text.DecompiledText
import org.jetbrains.kotlin.analysis.decompiler.psi.text.buildDecompiledText
import org.jetbrains.kotlin.analysis.decompiler.stub.file.ClsClassFinder
import org.jetbrains.kotlin.analysis.utils.errors.requireIsInstance
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.stubs.impl.KotlinFileStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.KotlinNameReferenceExpressionStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.KotlinUserTypeStubImpl
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled
import org.jetbrains.kotlin.utils.concurrent.block.LockedClearableLazyValue

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
        val clonedStub = stub.cloneRecursively()
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

// TODO: KT-80276 Implement native coping for stubs
/** Creates a deep copy of the given [this]. */
@KtImplementationDetail
fun KotlinFileStubImpl.cloneRecursively(): KotlinFileStubImpl {
    val clonedStub = cloneStubRecursively(
        originalStub = this,
        copyParentStub = null,
        buffer = UnsyncByteArrayOutputStream(),
        storage = StringEnumerator(),
    ) as KotlinFileStubImpl

    return clonedStub
}

/**
 * Returns a copy of [originalStub].
 */
private fun <T : PsiElement> cloneStubRecursively(
    originalStub: StubElement<T>,
    copyParentStub: StubElement<*>?,
    buffer: UnsyncByteArrayOutputStream,
    storage: AbstractStringEnumerator,
): StubElement<*> {
    buffer.reset()

    // Some specific elements are covered here as they widely used and has additional logic inside `serialize`,
    // to it is an optimization
    val copyStub = when (originalStub) {
        is KotlinUserTypeStubImpl -> KotlinUserTypeStubImpl(
            copyParentStub,
            originalStub.upperBound,
            originalStub.abbreviatedType,
        )

        is KotlinNameReferenceExpressionStubImpl -> KotlinNameReferenceExpressionStubImpl(
            copyParentStub,
            StringRef.fromString(originalStub.referencedName),
            originalStub.isClassRef,
        )

        is PsiFileStub -> {
            val serializer = originalStub.type
            serializer.serialize(originalStub, StubOutputStream(buffer, storage))
            serializer.deserialize(StubInputStream(buffer.toInputStream(), storage), copyParentStub)
        }

        else -> {
            val serializer = originalStub.stubType
            serializer.serialize(originalStub, StubOutputStream(buffer, storage))
            serializer.deserialize(StubInputStream(buffer.toInputStream(), storage), copyParentStub)
        }
    }

    for (originalChild in originalStub.childrenStubs) {
        cloneStubRecursively(originalStub = originalChild, copyParentStub = copyStub, buffer = buffer, storage = storage)
    }

    return copyStub
}

private class StringEnumerator : AbstractStringEnumerator {
    private val values = HashMap<String, Int>()
    private val strings = mutableListOf<String>()

    override fun enumerate(value: String?): Int {
        if (value == null) return 0

        return values.getOrPut(value) {
            strings += value
            values.size + 1
        }
    }

    override fun valueOf(idx: Int): String? = if (idx == 0) null else strings[idx - 1]

    override fun markCorrupted(): Unit = shouldNotBeCalled()
    override fun close(): Unit = shouldNotBeCalled()
    override fun isDirty(): Boolean = shouldNotBeCalled()
    override fun force(): Unit = shouldNotBeCalled()
}
