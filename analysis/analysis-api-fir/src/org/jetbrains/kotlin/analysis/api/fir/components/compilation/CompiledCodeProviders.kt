/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components.compilation

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findFile
import org.jetbrains.kotlin.codegen.state.CompiledCodeProvider

internal class KaFirDependencyCompiledCodeProvider(val cache: Map<String, ByteArray>) : CompiledCodeProvider {
    override fun getClassBytes(className: String): ByteArray? {
        return cache[className]
    }
}

internal class KaFirDelegatingCompiledCodeProvider(private val delegates: List<CompiledCodeProvider>) : CompiledCodeProvider {
    override fun getClassBytes(className: String): ByteArray? {
        return delegates.firstNotNullOfOrNull { it.getClassBytes(className) }
    }
}

internal class KaFirDirectoryBasedCompiledCodeProvider(private val outputDirectory: VirtualFile) : CompiledCodeProvider {
    override fun getClassBytes(className: String): ByteArray? {
        return outputDirectory.findFile("$className.class")?.contentsToByteArray(false)
    }
}
