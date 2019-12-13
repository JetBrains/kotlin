/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle

import com.intellij.openapi.externalSystem.service.project.autoimport.ConfigurationFileCrcFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.plugins.gradle.settings.GradleLocalSettings
import java.nio.file.Paths
import java.util.*

class GradleScriptInputsWatcher(val project: Project) {
    private val comparator = Comparator<VirtualFile> { f1, f2 -> (f1.timeStamp - f2.timeStamp).toInt() }

    private val storage = TreeSet(comparator)

    init {
        initStorage(project)
    }

    private fun initStorage(project: Project) {
        val localSettings = GradleLocalSettings.getInstance(project)
        localSettings.externalConfigModificationStamps.forEach { (path, stamp) ->
            val file = VfsUtil.findFile(Paths.get(path), true)
            if (file != null && !file.isDirectory) {
                val calculateCrc = ConfigurationFileCrcFactory(file).create()
                if (calculateCrc != stamp) {
                    addToStorage(file)
                }
            }
        }
    }

    fun lastModifiedFileTimeStamp(file: VirtualFile): Long = lastModifiedRelatedFile(file)?.timeStamp ?: 0

    fun areRelatedFilesUpToDate(file: VirtualFile, timeStamp: Long): Boolean {
        return lastModifiedFileTimeStamp(file) <= timeStamp
    }

    fun addToStorage(file: VirtualFile) {
        if (storage.contains(file)) {
            storage.remove(file)
        }
        storage.add(file)
    }

    private fun lastModifiedRelatedFile(file: VirtualFile): VirtualFile? {
        if (storage.isEmpty()) return null

        val iterator = storage.descendingIterator()
        if (!iterator.hasNext()) return null

        var lastModifiedFile = iterator.next()
        while (lastModifiedFile == file && iterator.hasNext()) {
            lastModifiedFile = iterator.next()
        }

        if (lastModifiedFile == file) return null

        return lastModifiedFile
    }
}