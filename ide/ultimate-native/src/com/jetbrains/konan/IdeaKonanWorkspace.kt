/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.konan

import com.intellij.execution.ExecutionTargetManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.project.Project

@State(name = "KotlinMultiplatform", storages = [(Storage(StoragePathMacros.WORKSPACE_FILE))])
class IdeaKonanWorkspace(project: Project) : WorkspaceBase(project) {
    init {
        val connection = project.messageBus.connect()
        connection.subscribe(ExecutionTargetManager.TOPIC, TargetListener(this))
    }

    companion object {
        @JvmStatic
        fun getInstance(project: Project): IdeaKonanWorkspace = project.getComponent(IdeaKonanWorkspace::class.java)
    }
}