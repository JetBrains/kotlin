/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.cli.jvm.index.JvmDependenciesIndex
import org.jetbrains.kotlin.cli.jvm.index.JvmIndexKotlinClassFinder
import org.jetbrains.kotlin.jvm.environment.JvmClasspath
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.VirtualFileBasedApi
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinder
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory
import org.jetbrains.kotlin.util.PerformanceManager

/**
 * Creates the finders of classes in [index]. The lookups which return a [com.intellij.openapi.vfs.VirtualFile] read [vfsIndex] instead, an
 * index over the same roots with VFS entries: [index] may have NIO entries, whose files have no `VirtualFile`.
 */
// TODO(KT-90203): create different JvmDependenciesIndex instances for different sets of source roots to improve performance
class CliVirtualFileFinderFactory(
    val index: JvmDependenciesIndex,
    val enableSearchInCtSym: Boolean,
    private val perfManager: PerformanceManager?,
    private val vfsIndex: JvmDependenciesIndex = index,
) : VirtualFileFinderFactory {
    /** Scope-based lookup required by the shared [VirtualFileFinderFactory] contract (Analysis API); the compiler uses [create] with [JvmClasspath]. */
    @OptIn(VirtualFileBasedApi::class)
    override fun create(scope: GlobalSearchScope): VirtualFileFinder =
        CliVirtualFileFinder(vfsIndex, { it in scope }, enableSearchInCtSym, perfManager)

    fun create(classpath: JvmClasspath): JvmIndexKotlinClassFinder =
        JvmIndexKotlinClassFinder(index, classpath, enableSearchInCtSym, perfManager)

    /** All roots of the indexes, filtered by root, not by [GlobalSearchScope]: the files of NIO root entries have no `VirtualFile`. */
    override fun create(project: Project): VirtualFileFinder =
        CliVirtualFileFinder(
            create(JvmClasspath.ProjectLibraries()),
            JvmIndexKotlinClassFinder(vfsIndex, JvmClasspath.ProjectLibraries(), enableSearchInCtSym, perfManager),
            perfManager,
        )
}
