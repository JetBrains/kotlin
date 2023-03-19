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

import org.jetbrains.kotlin.resolve.jvm.modules.JavaModule
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleFinder
import org.jetbrains.kotlin.storage.LockBasedStorageManager

class JavaModuleGraph(finder: JavaModuleFinder) {
    private val module: (String) -> JavaModule? =
        LockBasedStorageManager.NO_LOCKS.createMemoizedFunctionWithNullableValues(finder::findModule)

    fun getAllDependencies(moduleNames: List<String>): LinkedHashSet<String> {
        val visited = LinkedHashSet(moduleNames)

        // Every module implicitly depends on java.base
        visited += "java.base"

        fun dfs(moduleName: String): Boolean {
            // Automatic modules have no transitive exports, so we only consider explicit modules here
            val moduleInfo = (module(moduleName) as? JavaModule.Explicit)?.moduleInfo ?: return false
            for ((dependencyModuleName, isTransitive) in moduleInfo.requires) {
                if (isTransitive && visited.add(dependencyModuleName)) {
                    dfs(dependencyModuleName)
                }
            }
            return true
        }

        for (moduleName in moduleNames) {
            val module = module(moduleName) ?: continue
            when (module) {
                is JavaModule.Automatic -> {
                    // Do nothing; all automatic modules should be added to compilation roots at call site as per java.lang.module javadoc
                }
                is JavaModule.Explicit -> {
                    for ((dependencyModuleName, isTransitive) in module.moduleInfo.requires) {
                        if (visited.add(dependencyModuleName)) {
                            val moduleExists = dfs(dependencyModuleName)
                            //ct.sym can miss some internal modules from non-transitive dependencies
                            if (!moduleExists && !isTransitive && module.isJdkModuleFromCtSym) visited.remove(dependencyModuleName)
                        }
                    }
                }
                else -> error("Unknown module: $module (${module.javaClass})")
            }
        }

        return visited
    }

    fun reads(moduleName: String, dependencyName: String): Boolean {
        if (moduleName == dependencyName || dependencyName == "java.base") return true

        val visited = linkedSetOf<String>()

        fun dfs(name: String): Boolean {
            if (!visited.add(name)) return false

            val module = module(name) ?: return false
            when (module) {
                is JavaModule.Automatic -> return true
                is JavaModule.Explicit -> {
                    for ((dependencyModuleName, isTransitive) in module.moduleInfo.requires) {
                        if (dependencyModuleName == dependencyName) return true
                        if (isTransitive && dfs(dependencyName)) return true
                    }
                    return false
                }
                else -> error("Unsupported module type: $module")
            }
        }

        val module = module(moduleName) ?: return false
        when (module) {
            is JavaModule.Automatic -> return true
            is JavaModule.Explicit -> {
                for ((dependencyModuleName) in module.moduleInfo.requires) {
                    if (dfs(dependencyModuleName)) return true
                }
            }
        }

        return dfs(moduleName)
    }
}
