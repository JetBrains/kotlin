package com.jetbrains.cidr.apple.gradle

import com.intellij.util.messages.Topic

interface GradleAppleWorkspaceListener {
    companion object {
        @JvmField
        val TOPIC: Topic<GradleAppleWorkspaceListener> = Topic.create("GradleAppleWorkspace", GradleAppleWorkspaceListener::class.java)
    }

    fun workspaceUpdated()
}