/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.LibraryTable
import com.intellij.openapi.vfs.*
import org.jetbrains.konan.KotlinNativeToolchain
import org.jetbrains.konan.settings.KonanModelProvider.RELOAD_TOPIC
import org.jetbrains.kotlin.utils.addIfNotNull
import java.nio.file.Path

abstract class KonanProjectComponent(val project: Project) : ProjectComponent {
    companion object {
        fun getInstance(project: Project): KonanProjectComponent = project.getComponent(KonanProjectComponent::class.java)
    }

    protected var libraryPaths: Set<String> = emptySet()

    override fun getComponentName(): String = "Kotlin/Native Project Component"

    override fun projectOpened() {
        VirtualFileManager.getInstance().addVirtualFileListener(object : VirtualFileListener {
            override fun fileCreated(event: VirtualFileEvent) {
                val filePath = event.file.path
                if (libraryPaths.contains(filePath)) reloadLibraries()
            }
        }, project)

        val connection = project.messageBus.connect(project)
        connection.subscribe(RELOAD_TOPIC, Runnable {
            ApplicationManager.getApplication().invokeLater {
                KotlinNativeToolchain.BUNDLED.ensureExists(project)
                reloadLibraries()
            }
        })

        if (looksLikeKotlinNativeProject()) {
            reloadLibraries()
        }
    }

    open fun reloadLibraries(): Unit = synchronized(this) {
    }

    protected open fun collectLibraryPaths(): MutableSet<Path> {
        val konanPaths = KonanPaths.getInstance(project)
        return mutableSetOf<Path>().apply {
            addIfNotNull(konanPaths.konanStdlib())
            addAll(konanPaths.konanPlatformLibraries())
        }
    }

    abstract fun looksLikeKotlinNativeProject(): Boolean
}