/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.compiler.configuration

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "KotlinCompilerWorkspaceSettings",
    storages = [Storage(file = StoragePathMacros.WORKSPACE_FILE)]
)
class KotlinCompilerWorkspaceSettings : PersistentStateComponent<KotlinCompilerWorkspaceSettings> {
    /**
     * incrementalCompilationForJvmEnabled
     * (name `preciseIncrementalEnabled` is kept for workspace file compatibility)
     */
    var preciseIncrementalEnabled: Boolean = true
    var incrementalCompilationForJsEnabled: Boolean = true
    var enableDaemon: Boolean = true

    override fun getState(): KotlinCompilerWorkspaceSettings {
        return this
    }

    override fun loadState(state: KotlinCompilerWorkspaceSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        @JvmStatic
        fun getInstance(project: Project): KotlinCompilerWorkspaceSettings =
            ServiceManager.getService(project, KotlinCompilerWorkspaceSettings::class.java)
    }
}
