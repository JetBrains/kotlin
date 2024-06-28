/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
        friendPaths.add(File(path).absolutePath)
    }

    override fun dispose() {
        chunk.clear()
    }
}
