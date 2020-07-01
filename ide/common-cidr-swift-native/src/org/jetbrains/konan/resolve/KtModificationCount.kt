package org.jetbrains.konan.resolve

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SimpleModificationTracker

class KtModificationCount {
    companion object {
        @JvmStatic
        fun getInstance(project: Project): KtModificationCount = project.service()
    }

    private val myOutOfCodeBlockModificationTracker = SimpleModificationTracker()

    fun inc() {
        myOutOfCodeBlockModificationTracker.incModificationCount()
    }

    fun get(): Long = myOutOfCodeBlockModificationTracker.modificationCount
}