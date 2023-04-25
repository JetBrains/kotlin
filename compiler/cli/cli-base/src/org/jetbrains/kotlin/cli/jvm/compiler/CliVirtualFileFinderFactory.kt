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
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinder
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory

// TODO: create different JvmDependenciesIndex instances for different sets of source roots to improve performance
class CliVirtualFileFinderFactory(private val index: JvmDependenciesIndex, private val enableSearchInCtSym: Boolean) : VirtualFileFinderFactory {
    override fun create(scope: GlobalSearchScope): VirtualFileFinder = CliVirtualFileFinder(index, scope, enableSearchInCtSym)

    override fun create(project: Project, module: ModuleDescriptor): VirtualFileFinder =
        CliVirtualFileFinder(index, GlobalSearchScope.allScope(project), enableSearchInCtSym)
}
