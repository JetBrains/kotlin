/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.cli.jvm.modules

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModule
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleFinder

class CliJavaModuleFinder(jrtFileSystemRoot: VirtualFile?) : JavaModuleFinder {
    private val modulesRoot = jrtFileSystemRoot?.findChild("modules")
    private val userModules = linkedMapOf<String, JavaModule>()

    fun addUserModule(module: JavaModule) {
        userModules.putIfAbsent(module.name, module)
    }

    val allObservableModules: Sequence<JavaModule>
        get() = systemModules + userModules.values

    val systemModules: Sequence<JavaModule.Explicit>
        get() = modulesRoot?.children.orEmpty().asSequence().mapNotNull(this::findSystemModule)

    override fun findModule(name: String): JavaModule? =
            modulesRoot?.findChild(name)?.let(this::findSystemModule) ?: userModules[name]

    private fun findSystemModule(moduleRoot: VirtualFile): JavaModule.Explicit? {
        /*
        val file = moduleRoot.findChild(PsiJavaModule.MODULE_INFO_CLS_FILE) ?: return null
        val moduleInfo = JavaModuleInfo.read(file) ?: return null
        return JavaModule.Explicit(moduleInfo, listOf(JavaModule.Root(moduleRoot, isBinary = true)), file)
        */
        return null
    }
}
