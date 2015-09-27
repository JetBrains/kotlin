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
import org.jetbrains.kotlin.cli.common.modules.ModuleXmlParser
import org.jetbrains.kotlin.descriptors.*
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
            }
            else {
                (from as? LazyPackageDescriptor)?.declarationProvider?.getPackageFiles()?.firstOrNull()?.project ?: return true
            }

        val moduleVisibilityManager = ModuleVisibilityManager.SERVICE.getInstance(project)

        val whatSource = getSourceElement(what)
        if (whatSource is KotlinSourceElement) return true

        val modules = moduleVisibilityManager.chunk.toList()
        val outputDirectories = modules.map { File(it.getOutputDirectory()) }
        if (outputDirectories.isEmpty()) return isContainedByCompiledPartOfOurModule(what, null)

        outputDirectories.forEach {
            if (isContainedByCompiledPartOfOurModule(what, it)) return true
        }

        // Hack in order to allow access to internal elements in production code from tests
        if (modules.size() == 1 && modules[0].getModuleType() == ModuleXmlParser.TYPE_TEST) return true

        return false
    }
}

/*
   At the moment, there is no proper support for module infrastructure in the compiler.
   So we add try to remember given list of interdependent modules and use it for checking visibility.
 */
class CliModuleVisibilityManagerImpl() : ModuleVisibilityManager, Disposable {
    override val chunk: MutableList<Module> = arrayListOf()

    override fun addModule(module: Module) {
        chunk.add(module)
    }

    override fun dispose() {
        chunk.clear()
    }
}