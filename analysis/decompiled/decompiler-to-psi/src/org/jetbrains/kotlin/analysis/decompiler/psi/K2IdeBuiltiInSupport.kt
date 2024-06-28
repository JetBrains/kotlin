/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.psi

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.analysis.decompiler.stub.file.KotlinMetadataStubBuilder

/**
 * Application service that adds a constant offset to the stub version of .kotlin_builtins files.
 * The purpose of this offset is to rebuild the decompiled text and the stubs for .kotlin_builtins files after K1 <-> K2 IDE switches.
 * K1 and K2 provide different sets of declarations from .kotlin_builtins files under certain conditions,
 * see [org.jetbrains.kotlin.analysis.decompiler.psi.BuiltInDefinitionFile].
 * Not forcing a rebuild for affected decompiled files and corresponding stubs leads to a stub error.
 */
interface KotlinBuiltInStubVersionOffsetProvider {
    fun getVersionOffset(): Int

    companion object {
        fun getVersionOffset(): Int =
            ApplicationManager.getApplication().getService(KotlinBuiltInStubVersionOffsetProvider::class.java)?.getVersionOffset() ?: 0
    }
}

interface KotlinBuiltInDecompilationInterceptor {
    fun readFile(bytes: ByteArray, file: VirtualFile): KotlinMetadataStubBuilder.FileWithMetadata?

    companion object {
        fun readFile(bytes: ByteArray, file: VirtualFile): KotlinMetadataStubBuilder.FileWithMetadata? =
            ApplicationManager.getApplication().getService(KotlinBuiltInDecompilationInterceptor::class.java)?.readFile(bytes, file)
    }
}
