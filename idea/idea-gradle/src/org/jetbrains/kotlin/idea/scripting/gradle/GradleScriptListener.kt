/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.core.script.configuration.listener.ScriptChangeListener
import org.jetbrains.kotlin.idea.scripting.gradle.legacy.GradleLegacyScriptListener
import org.jetbrains.kotlin.idea.scripting.gradle.roots.GradleBuildRootsManager

class GradleScriptListener(project: Project) : ScriptChangeListener(project) {
    // todo(gradle6): remove
    private val legacy = GradleLegacyScriptListener(project)

    private val buildRootsManager
        get() = GradleBuildRootsManager.getInstance(project)

    init {
        // listen changes using VFS events, including gradle-configuration related files
        addVfsListener(this, buildRootsManager)
    }

    // cache buildRootsManager service for hot path under vfs changes listener
    val fileChangesProcessor: (filePath: String, ts: Long) -> Unit
        get() {
            val buildRootsManager = buildRootsManager
            return { filePath, ts ->
                buildRootsManager.fileChanged(filePath, ts)
            }
        }

    fun fileChanged(filePath: String, ts: Long) =
        fileChangesProcessor(filePath, ts)

    override fun isApplicable(vFile: VirtualFile) =
        // todo(gradle6): replace with `isCustomScriptingSupport(vFile)`
        legacy.isApplicable(vFile)

    private fun isCustomScriptingSupport(vFile: VirtualFile) =
        buildRootsManager.isApplicable(vFile)

    override fun editorActivated(vFile: VirtualFile) {
        if (isCustomScriptingSupport(vFile)) {
            buildRootsManager.updateNotifications(restartAnalyzer = false) { it == vFile.path }
        } else {
            legacy.editorActivated(vFile)
        }
    }

    override fun documentChanged(vFile: VirtualFile) {
        fileChanged(vFile.path, System.currentTimeMillis())

        if (!isCustomScriptingSupport(vFile)) {
            legacy.documentChanged(vFile)
        }
    }
}