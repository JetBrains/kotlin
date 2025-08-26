/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base.declarations

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.CollectionFactory
import org.jetbrains.kotlin.psi.stubs.impl.KotlinFileStubImpl

/**
 * Test application service to store stubs of libraries shared between tests.
 *
 * Otherwise, each test would start indexing of stdlib from scratch and under the lock, which makes tests extremely slow.
 *
 * The cache shouldn't be stored on hard references to avoid problems with memory leaks.
 *
 * **Note**: shared stubs might store psi, but **MUST NOT** reuse it for different files
 */
internal class KotlinStandaloneStubsCache {
    private val stubs = CollectionFactory.createConcurrentWeakKeySoftValueMap<VirtualFile, KotlinFileStubImpl>()

    fun getOrBuildStub(
        compiledFile: VirtualFile,
        stubBuilder: (VirtualFile) -> KotlinFileStubImpl,
    ): KotlinFileStubImpl = stubs.computeIfAbsent(compiledFile, stubBuilder)
}
