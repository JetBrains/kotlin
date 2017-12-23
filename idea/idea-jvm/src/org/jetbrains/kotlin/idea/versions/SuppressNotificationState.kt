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

package org.jetbrains.kotlin.idea.versions

import com.intellij.openapi.components.*
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil
import org.jetbrains.kotlin.idea.configuration.ModuleSourceRootGroup
import org.jetbrains.kotlin.idea.configuration.toModuleGroup

@State(name = "SuppressABINotification", storages = arrayOf(Storage(file = StoragePathMacros.PROJECT_FILE)))
class SuppressNotificationState : PersistentStateComponent<SuppressNotificationState> {
    var isSuppressed: Boolean = false
    var modulesWithSuppressedNotConfigured = sortedSetOf<String>()

    override fun getState(): SuppressNotificationState = this

    override fun loadState(state: SuppressNotificationState) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        fun getInstance(project: Project) = ServiceManager.getService(project, SuppressNotificationState::class.java)

        fun isKotlinNotConfiguredSuppressed(moduleGroup: ModuleSourceRootGroup): Boolean {
            val baseModule = moduleGroup.baseModule
            return baseModule.name in getInstance(baseModule.project).modulesWithSuppressedNotConfigured
        }

        fun suppressKotlinNotConfigured(module: Module) {
            getInstance(module.project).modulesWithSuppressedNotConfigured.add(module.toModuleGroup().baseModule.name)
        }
    }
}
