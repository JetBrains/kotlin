/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.cli.common

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.load.kotlin.ModuleVisibilityManager
import org.jetbrains.kotlin.load.kotlin.getSourceElement
import org.jetbrains.kotlin.load.kotlin.isContainedByCompiledPartOfOurModule
import org.jetbrains.kotlin.modules.Module
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyPackageDescriptor
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement
import org.jetbrains.kotlin.util.ModuleVisibilityHelper
import java.io.File

class ModuleVisibilityHelperImpl : ModuleVisibilityHelper {
    override fun isInFriendModule(what: DeclarationDescriptor, from: DeclarationDescriptor): Boolean {
        val fromSource = getSourceElement(from)
        // We should check accessibility of 'from' in current module (some set of source files, which are compiled together),
        // so we can assume that 'from' should have sources or is a LazyPackageDescriptor with some package files.
        val project: Project = if (fromSource is KotlinSourceElement) {
            fromSource.psi.project
        } else {
            (from as? LazyPackageDescriptor)?.declarationProvider?.getPackageFiles()?.firstOrNull()?.project ?: return true
        }

        val moduleVisibilityManager = ModuleVisibilityManager.SERVICE.getInstance(project)
        if (!moduleVisibilityManager.enabled) return true

        moduleVisibilityManager.friendPaths.forEach {
            if (isContainedByCompiledPartOfOurModule(what, File(it))) return true
        }

        val modules = moduleVisibilityManager.chunk

        val whatSource = getSourceElement(what)
        if (whatSource is KotlinSourceElement) {
            if (modules.size > 1 && fromSource is KotlinSourceElement) {
                return findModule(what, modules) === findModule(from, modules)
            }

            return true
        }

        if (modules.isEmpty()) return false

        if (modules.size == 1 && isContainedByCompiledPartOfOurModule(what, File(modules.single().getOutputDirectory()))) return true

        return findModule(from, modules) === findModule(what, modules)
    }

    private fun findModule(descriptor: DeclarationDescriptor, modules: Collection<Module>): Module? {
        val sourceElement = getSourceElement(descriptor)
        return if (sourceElement is KotlinSourceElement) {
            modules.singleOrNull() ?: modules.firstOrNull { sourceElement.psi.containingKtFile.virtualFile.path in it.getSourceFiles() }
        } else {
            modules.firstOrNull { module ->
                isContainedByCompiledPartOfOurModule(descriptor, File(module.getOutputDirectory())) ||
                        module.getFriendPaths().any { isContainedByCompiledPartOfOurModule(descriptor, File(it)) }
            }
        }
    }
}

/*
   At the moment, there is no proper support for module infrastructure in the compiler.
   So we add try to remember given list of interdependent modules and use it for checking visibility.
 */
class CliModuleVisibilityManagerImpl(override val enabled: Boolean) : ModuleVisibilityManager, Disposable {
    override val chunk: MutableList<Module> = arrayListOf()
    override val friendPaths: MutableList<String> = arrayListOf()

    override fun addModule(module: Module) {
        chunk.add(module)
    }

    override fun addFriendPath(path: String) {
        friendPaths.add(path)
    }

    override fun dispose() {
        chunk.clear()
    }
}
