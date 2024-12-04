/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.services

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.analysis.decompiler.psi.BuiltInDefinitionFile
import org.jetbrains.kotlin.analysis.decompiler.psi.BuiltinsVirtualFileProvider
import org.jetbrains.kotlin.analysis.decompiler.psi.KotlinBuiltInDecompilationInterceptor
import org.jetbrains.kotlin.analysis.decompiler.stub.file.KotlinMetadataStubBuilder

internal class KotlinBuiltInDecompilationInterceptorTestImpl : KotlinBuiltInDecompilationInterceptor {
    override fun readFile(bytes: ByteArray, file: VirtualFile): KotlinMetadataStubBuilder.FileWithMetadata? {
        return if (file in BuiltinsVirtualFileProvider.getInstance().getBuiltinVirtualFiles())
            BuiltInDefinitionFile.read(bytes, file, filterOutClassesExistingAsClassFiles = false)
        else null
    }
}