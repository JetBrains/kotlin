/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

@State(name = "KotlinCodeInsightWorkspaceSettings", storages = [Storage("kotlinCodeInsightSettings.xml")])
class KotlinCodeInsightWorkspaceSettings : PersistentStateComponent<KotlinCodeInsightWorkspaceSettings> {

    @JvmField
    var addUnambiguousImportsOnTheFly = false

    @JvmField
    var optimizeImportsOnTheFly = false

    override fun getState() = this

    override fun loadState(state: KotlinCodeInsightWorkspaceSettings) = XmlSerializerUtil.copyBean(state, this)

    companion object {

        fun getInstance(project: Project): KotlinCodeInsightWorkspaceSettings {
            return ServiceManager.getService(project, KotlinCodeInsightWorkspaceSettings::class.java)
        }

    }

}