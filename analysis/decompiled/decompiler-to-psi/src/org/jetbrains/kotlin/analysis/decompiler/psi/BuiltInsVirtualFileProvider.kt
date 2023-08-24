/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.psi

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import java.net.URL

abstract class BuiltInsVirtualFileProvider {
    abstract fun getBuiltInVirtualFiles(): Set<VirtualFile>

    companion object {
        fun getInstance(): BuiltInsVirtualFileProvider =
            ApplicationManager.getApplication().getService(BuiltInsVirtualFileProvider::class.java)
    }
}

abstract class BuiltInsVirtualFileProviderBaseImpl : BuiltInsVirtualFileProvider() {
    private val builtinFiles: Set<VirtualFile> by lazy {
        val classLoader = this::class.java.classLoader
        StandardClassIds.builtInsPackages.mapTo(mutableSetOf()) { builtInPackageFqName ->
            val resourcePath = BuiltInSerializerProtocol.getBuiltInsFilePath(builtInPackageFqName)
            val resourceUrl = classLoader.getResource(resourcePath)
                ?: errorWithAttachment("Resource for builtin $builtInPackageFqName not found") {
                    withEntry("resourcePath", resourcePath)
                }
            findVirtualFile(resourceUrl)
                ?: errorWithAttachment("Virtual file for builtin $builtInPackageFqName not found") {
                    withEntry("resourcePath", resourcePath)
                    withEntry("resourceUrl", resourceUrl) { it.toString() }
                }
        }
    }

    protected abstract fun findVirtualFile(url: URL): VirtualFile?

    override fun getBuiltInVirtualFiles(): Set<VirtualFile> = builtinFiles
}


