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

package org.jetbrains.kotlin.resolve.jvm.modules

import org.jetbrains.kotlin.storage.LockBasedStorageManager

class JavaModuleGraph(finder: JavaModuleFinder) {
    private val module: (String) -> JavaModule? =
            LockBasedStorageManager.NO_LOCKS.createMemoizedFunctionWithNullableValues(finder::findModule)

    fun getAllDependencies(moduleNames: List<String>): List<String> {
        val visited = moduleNames.toMutableSet()

        // Every module implicitly depends on java.base
        visited += "java.base"

        fun dfs(moduleName: String) {
            // Automatic modules have no transitive exports, so we only consider explicit modules here
            val moduleInfo = (module(moduleName) as? JavaModule.Explicit)?.moduleInfo ?: return
            for ((dependencyModuleName, isTransitive) in moduleInfo.requires) {
                if (!visited.add(dependencyModuleName) && isTransitive) {
                    dfs(dependencyModuleName)
                }
            }
        }

        moduleNames.forEach(::dfs)
        return visited.toList()
    }

    fun reads(moduleName: String, dependencyName: String): Boolean {
        if (moduleName == dependencyName || dependencyName == "java.base") return true

        val visited = linkedSetOf<String>()

        fun dfs(name: String): Boolean {
            if (!visited.add(name)) return false

            val module = module(name)
            when (module) {
                is JavaModule.Automatic -> return true
                is JavaModule.Explicit -> {
                    for ((dependencyModuleName, isTransitive) in module.moduleInfo.requires) {
                        if (dependencyModuleName == dependencyName) return true
                        if (isTransitive && dfs(dependencyName)) return true
                    }
                    return false
                }
                null -> return false
                else -> error("Unsupported module type: $module")
            }
        }

        return dfs(moduleName)
    }
}
