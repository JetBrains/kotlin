/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.configuration.listener

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.idea.core.script.configuration.DefaultScriptingSupport
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.KtFile

/**
 * [ScriptChangesNotifier] will call first applicable [ScriptChangeListener] when editor is activated or document changed.
 * Listener should do something to invalidate configuration and schedule reloading.
 */
abstract class ScriptChangeListener(val project: Project) {
    val default: DefaultScriptingSupport
        get() = DefaultScriptingSupport.getInstance(project)

    abstract fun editorActivated(vFile: VirtualFile)
    abstract fun documentChanged(vFile: VirtualFile)

    abstract fun isApplicable(vFile: VirtualFile): Boolean

    protected fun getAnalyzableKtFileForScript(vFile: VirtualFile): KtFile? {
        return runReadAction {
            if (project.isDisposed) return@runReadAction null
            if (!vFile.isValid) return@runReadAction null
            (PsiManager.getInstance(project).findFile(vFile) as? KtFile)?.takeIf {
                ProjectRootsUtil.isInProjectSource(it, includeScriptsOutsideSourceRoots = true)
            }
        }
    }

    companion object {
        val LISTENER: ExtensionPointName<ScriptChangeListener> =
            ExtensionPointName.create("org.jetbrains.kotlin.scripting.idea.listener")
    }
}