/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(KtImplementationDetail::class)

package org.jetbrains.kotlin.analysis.api.standalone.fir.declarations

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.compiled.ClassFileDecompilers
import com.intellij.util.containers.CollectionFactory
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.stubs.impl.KotlinFileStubImpl

/**
 * Test application service to store libraries' information shared between tests.
 *
 * Otherwise, each test would start indexing of stdlib from scratch and under the lock, which makes tests extremely slow.
 *
 * The cache shouldn't be stored on hard references to avoid problems with memory leaks.
 *
 * **Note**: shared stubs might store psi, but **MUST NOT** reuse it for different files
 */
internal class KotlinStandaloneIndexCache {
    private val stubs = CollectionFactory.createConcurrentWeakKeySoftValueMap<VirtualFile, KotlinFileStubImpl>()
    private val packageNames = CollectionFactory.createConcurrentWeakKeySoftValueMap<VirtualFile, FqName>()
    private val indexRecords = CollectionFactory.createConcurrentWeakKeySoftValueMap<VirtualFile, KotlinStandaloneFileIndexRecords>()
    private val roots = CollectionFactory.createConcurrentWeakKeySoftValueMap<VirtualFile, Set<SharedIndexableDecompiledFile>>()

    fun getOrBuildStub(
        compiledFile: VirtualFile,
        stubBuilder: (VirtualFile) -> KotlinFileStubImpl,
    ): KotlinFileStubImpl = stubs.computeIfAbsent(compiledFile, stubBuilder)

    /**
     * Computing the package name of a decompiled file requires its stub,
     * so without this cache each test would build stubs for all library files.
     */
    fun getOrComputePackageName(
        compiledFile: VirtualFile,
        packageNameComputer: (VirtualFile) -> FqName,
    ): FqName = packageNames.computeIfAbsent(compiledFile, packageNameComputer)

    /**
     * The records refer to the indexed elements only by their ordinals, so they can be shared between projects.
     */
    fun getIndexRecords(compiledFile: VirtualFile): KotlinStandaloneFileIndexRecords? = indexRecords[compiledFile]

    fun putIndexRecords(compiledFile: VirtualFile, records: KotlinStandaloneFileIndexRecords) {
        indexRecords[compiledFile] = records
    }

    /** @see SharedIndexableDecompiledFile */
    fun getOrProcessBinaryRoot(
        binaryRoot: VirtualFile,
        processRoot: (VirtualFile) -> Set<SharedIndexableDecompiledFile>,
    ): Set<SharedIndexableDecompiledFile> = roots.computeIfAbsent(binaryRoot) { processRoot(it) }

    /**
     * Represents a binary file successfully decompiled by some Kotlin decompiler.
     */
    class SharedIndexableDecompiledFile(
        val virtualFile: VirtualFile,
        val kotlinDecompiler: ClassFileDecompilers.Full,
    ) {
        override fun equals(other: Any?): Boolean = this === other ||
                other is SharedIndexableDecompiledFile && virtualFile == other.virtualFile

        override fun hashCode(): Int = virtualFile.hashCode()
        override fun toString(): String = "$virtualFile (${kotlinDecompiler::class.simpleName})"
    }
}
