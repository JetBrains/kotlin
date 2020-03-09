/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.konan

import com.intellij.execution.*
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import org.jdom.Element
import org.jetbrains.kotlin.konan.CompilerVersion
import org.jetbrains.kotlin.konan.CompilerVersionImpl
import org.jetbrains.kotlin.konan.MetaVersion
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.util.DependencyDirectories
import java.io.File
import java.util.*
import kotlin.collections.HashSet

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