package com.jetbrains.konan

import com.intellij.openapi.application.ApplicationManager
import javax.swing.Icon

interface KonanIconProvider {
    fun getExecutableIcon(): Icon

    companion object {
        fun getInstance(): KonanIconProvider = ApplicationManager.getApplication().getComponent(KonanIconProvider::class.java)
    }
}