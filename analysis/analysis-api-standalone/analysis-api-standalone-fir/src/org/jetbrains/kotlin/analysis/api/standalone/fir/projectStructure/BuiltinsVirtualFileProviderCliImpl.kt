/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.fir.projectStructure

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.BinaryLightVirtualFile
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.decompiler.psi.BuiltinsVirtualFileProviderBaseImpl
import org.jetbrains.kotlin.psi.KtPlatformInterface
import java.net.URL

@OptIn(KtPlatformInterface::class)
@KaImplementationDetail
class BuiltinsVirtualFileProviderCliImpl : BuiltinsVirtualFileProviderBaseImpl() {
    private val files: Set<VirtualFile> by lazy { super.getBuiltinVirtualFiles() }

    override fun getBuiltinVirtualFiles(): Set<VirtualFile> = files

    override fun findVirtualFile(url: URL): VirtualFile {
        // With the default caching, the JDK would keep the JAR of the resource open
        val content = url.openConnection().apply { useCaches = false }.getInputStream().use { it.readBytes() }
        return BinaryLightVirtualFile(url.path.substringAfterLast('/'), content)
    }
}
