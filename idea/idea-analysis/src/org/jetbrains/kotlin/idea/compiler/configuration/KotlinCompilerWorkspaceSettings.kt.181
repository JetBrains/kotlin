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

package org.jetbrains.kotlin.idea.compiler.configuration

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
        name = "KotlinCompilerWorkspaceSettings",
        storages = arrayOf(
                Storage(file = StoragePathMacros.WORKSPACE_FILE)
        )
) class KotlinCompilerWorkspaceSettings : PersistentStateComponent<KotlinCompilerWorkspaceSettings> {
    var preciseIncrementalEnabled: Boolean = true
    var enableDaemon: Boolean = true

    override fun getState(): KotlinCompilerWorkspaceSettings {
        return this
    }

    override fun loadState(state: KotlinCompilerWorkspaceSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }
}
