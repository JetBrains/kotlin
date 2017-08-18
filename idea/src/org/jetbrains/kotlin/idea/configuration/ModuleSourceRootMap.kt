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

package org.jetbrains.kotlin.idea.configuration

import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.util.projectStructure.allModules

class ModuleSourceRootGroup(val baseModule: Module,
                            val sourceRootModules: List<Module>)

class ModuleSourceRootMap(val modules: Collection<Module>) {
    private val baseModuleByExternalPath: Map<String, Module>
    private val allModulesByExternalPath: Map<String, List<Module>>

    constructor(project: Project): this(project.allModules())

    init {
        allModulesByExternalPath = modules
                .filter { it.externalProjectPath != null && it.externalProjectId != null }
                .groupBy { it.externalProjectPath!! }

        baseModuleByExternalPath = allModulesByExternalPath
                .mapValues { (path, modules) ->
                    modules.reduce { m1, m2 ->
                        if (isSourceRootPrefix(m2.externalProjectId!!, m1.externalProjectId!!)) m2 else m1
                    }
                }
    }

    fun groupByBaseModules(modules: Collection<Module>): List<ModuleSourceRootGroup> {
        return modules
                .groupBy { module ->
                    val externalPath = module.externalProjectPath
                    if (externalPath == null) module else (baseModuleByExternalPath[externalPath] ?: module)
                }
                .map { (module, sourceRootModules) ->
                    ModuleSourceRootGroup(module,
                                          if (sourceRootModules.size > 1) sourceRootModules - module else sourceRootModules)
                }
    }

    fun toModuleGroup(module: Module): ModuleSourceRootGroup = groupByBaseModules(listOf(module)).single()
}

fun Module.toModuleGroup() = ModuleSourceRootMap(project).toModuleGroup(this)

private fun isSourceRootPrefix(externalId: String, previousModuleExternalId: String)
        = externalId.length < previousModuleExternalId.length && previousModuleExternalId.startsWith(externalId)

val Module.externalProjectId: String?
    get() = ExternalSystemApiUtil.getExternalProjectId(this)

val Module.externalProjectPath: String?
    get() = ExternalSystemApiUtil.getExternalProjectPath(this)

fun List<ModuleSourceRootGroup>.exclude(excludeModules: Collection<Module>): List<ModuleSourceRootGroup> {
    return mapNotNull {
        if (it.baseModule in excludeModules)
            null
        else {
            val remainingSourceRootModules = it.sourceRootModules - excludeModules
            if (remainingSourceRootModules.isEmpty())
                null
            else
                ModuleSourceRootGroup(it.baseModule, remainingSourceRootModules)
        }
    }
}