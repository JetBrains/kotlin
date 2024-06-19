/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.services

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.BinaryLightVirtualFile
import org.jetbrains.kotlin.analysis.decompiler.psi.BuiltinsVirtualFileProvider
import org.jetbrains.kotlin.analysis.decompiler.psi.BuiltinsVirtualFileProviderCliImpl

/**
 * In production (IDE & Standalone modes), builtins usually reside in the
 * stdlib, which is bundled into the IDE/Standalone mode application. The
 * JVM stdlib used in the project being analyzed by the Analysis API is a
 * different stdlib.
 *
 * This class allows providing different implementations
 * for stdlib builtins and builtins from [org.jetbrains.kotlin.analysis.api.projectStructure.KaBuiltinsModule]
 * For `KaBuiltinsModule`, we now create VirtualFiles in the air. This allows us to
 * decompile our builtins in both cases differently. See KT-61757 for the context.
 */
internal class BuiltinsVirtualFileProviderTestImpl() : BuiltinsVirtualFileProvider() {
    private val coreVirtualFileProvider = BuiltinsVirtualFileProviderCliImpl()

    private val files by lazy {
        coreVirtualFileProvider.getBuiltinVirtualFiles().mapTo(mutableSetOf()) { file ->
            BinaryLightVirtualFile(file.name, file.contentsToByteArray())
        }
    }

    override fun getBuiltinVirtualFiles(): Set<VirtualFile> = files

    override fun createBuiltinsScope(project: Project): GlobalSearchScope =
        GlobalSearchScope.filesScope(project, files)
}


