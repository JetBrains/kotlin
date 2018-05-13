/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "KotlinScriptingSettings",
    storages = [Storage("kotlinScripting.xml")]
)
class KotlinScriptingSettings : PersistentStateComponent<KotlinScriptingSettings> {
    var isAutoReloadEnabled = false

    override fun loadState(element: KotlinScriptingSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    override fun getState(): KotlinScriptingSettings {
        return this
    }

    companion object {
        fun getInstance(project: Project): KotlinScriptingSettings =
            ServiceManager.getService(project, KotlinScriptingSettings::class.java)
    }
}