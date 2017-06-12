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
    private val moduleInfo: (String) -> JavaModuleInfo? =
            LockBasedStorageManager.NO_LOCKS.createMemoizedFunctionWithNullableValues(finder::findModule)

    fun getAllDependencies(moduleNames: List<String>): List<String> {
        // Every module implicitly depends on java.base
        val visited = linkedSetOf("java.base")

        fun dfs(module: String) {
            if (!visited.add(module)) return
            val moduleInfo = moduleInfo(module) ?: return
            for (dependency in moduleInfo.requires) {
                if (dependency.isTransitive) {
                    dfs(dependency.moduleName)
                }
            }
        }

        moduleNames.forEach(::dfs)
        return visited.toList()
    }
}
