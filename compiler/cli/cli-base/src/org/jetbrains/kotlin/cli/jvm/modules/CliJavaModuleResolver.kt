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

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModule


class CliJavaModuleResolver(
    moduleGraph: JavaModuleGraph,
    private val userModules: List<JavaModule>,
    systemModules: List<JavaModule.Explicit>,
    project: Project
) : AbstractJavaModuleResolver(moduleGraph, systemModules, project) {
    init {
        assert(userModules.count(JavaModule::isSourceModule) <= 1) {
            "Modules computed by ClasspathRootsResolver cannot have more than one source module: $userModules"
        }
    }

    private val sourceModule: JavaModule? = userModules.firstOrNull(JavaModule::isSourceModule)

    override fun findSourceModule(file: VirtualFile): JavaModule? = sourceModule

    override fun findBinaryModule(file: VirtualFile): JavaModule? = userModules.firstOrNull { module -> file in module }
}
