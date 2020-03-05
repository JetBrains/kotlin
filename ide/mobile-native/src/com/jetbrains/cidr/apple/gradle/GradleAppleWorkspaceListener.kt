/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.cidr.apple.gradle

import com.intellij.util.messages.Topic

interface GradleAppleWorkspaceListener {
    companion object {
        @JvmField
        val TOPIC: Topic<GradleAppleWorkspaceListener> = Topic.create("GradleAppleWorkspace", GradleAppleWorkspaceListener::class.java)
    }

    fun workspaceUpdated()
}