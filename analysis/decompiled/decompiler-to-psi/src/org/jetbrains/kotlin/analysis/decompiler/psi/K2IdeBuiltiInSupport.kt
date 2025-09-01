/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.psi

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.serviceOrNull
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
            ApplicationManager.getApplication()
                .serviceOrNull<KotlinBuiltInStubVersionOffsetProvider>()
                ?.getVersionOffset()
                ?: 0
    }
}

internal interface KotlinBuiltInDecompilationInterceptor {
    fun readFile(bytes: ByteArray, file: VirtualFile): KotlinMetadataStubBuilder.FileWithMetadata?

    companion object {
        fun readFile(bytes: ByteArray, file: VirtualFile): KotlinMetadataStubBuilder.FileWithMetadata? =
            ApplicationManager.getApplication()
                .serviceOrNull<KotlinBuiltInDecompilationInterceptor>()
                ?.readFile(bytes, file)
    }
}

/**
 * Decompiles .kotlin_builtins files that belong to the kotlin-stdlib from the plugin classpath without class filtering.
 * The decompiled classes from these files are used in the symbol provider for built-ins in all modules, including non-JVM.
 * For common modules in particular, the lack of these classes leads to unresolved code, as the declarations are not published
 * in .kotlin_metadata files of kotlin-stdlib-common.
 * See [BuiltInDefinitionFile].
 */
private class K2KotlinBuiltInDecompilationInterceptor : KotlinBuiltInDecompilationInterceptor {
    override fun readFile(
        bytes: ByteArray,
        file: VirtualFile,
    ): KotlinMetadataStubBuilder.FileWithMetadata? = if (file in BuiltinsVirtualFileProvider.getInstance().getBuiltinVirtualFiles()) {
        BuiltInDefinitionFile.read(bytes, file, filterOutClassesExistingAsClassFiles = false)
    } else {
        null
    }
}
