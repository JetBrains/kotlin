/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.actions.internal.refactoringTesting

import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.gradle.getMethodOrNull

internal fun <T> lazyPub(initializer: () -> T) = lazy(LazyThreadSafetyMode.PUBLICATION, initializer)

internal fun gitReset(project: Project, projectRoot: VirtualFile) {

    fun ClassLoader.loadClassOrThrow(name: String) =
        loadClass(name) ?: error("$name not loaded")

    fun Class<*>.loadMethodOrThrow(name: String, vararg arguments: Class<*>) =
        getMethodOrNull(name, *arguments) ?: error("${this.name}::$name not loaded")

    val loader = PluginManager.getPlugin(PluginId.getId("Git4Idea"))?.pluginClassLoader ?: error("Git plugin is not found")

    val gitCls = loader.loadClassOrThrow("git4idea.commands.Git")
    val gitLineHandlerCls = loader.loadClassOrThrow("git4idea.commands.GitLineHandler")
    val gitCommandCls = loader.loadClassOrThrow("git4idea.commands.GitCommand")
    val gitCommandResultCls = loader.loadClassOrThrow("git4idea.commands.GitCommandResult")
    val gitLineHandlerCtor = gitLineHandlerCls.getConstructor(Project::class.java, VirtualFile::class.java, gitCommandCls) ?: error(
        "git4idea.commands.GitLineHandler::ctor not loaded"
    )
    val runCommand = gitCls.loadMethodOrThrow("runCommand", gitLineHandlerCls)
    val getExitCode = gitCommandResultCls.loadMethodOrThrow("getExitCode")
    val gitLineHandlerAddParameters = gitLineHandlerCls.loadMethodOrThrow("addParameters", List::class.java)

    val gitCommandReset = gitCommandCls.getField("RESET")?.get(null) ?: error("git4idea.commands.GitCommand.RESET not loaded")

    val resetLineHandler = gitLineHandlerCtor.newInstance(project, projectRoot, gitCommandReset)
    gitLineHandlerAddParameters.invoke(resetLineHandler, listOf("--hard", "HEAD"))

    val gitService = ServiceManager.getService(gitCls)
    val runCommandResult = runCommand.invoke(gitService, resetLineHandler)

    val gitResetResultCode = getExitCode.invoke(runCommandResult) as Int
    if (gitResetResultCode == 0) {
        VfsUtil.markDirtyAndRefresh(false, true, false, projectRoot)
        //GitRepositoryManager.getInstance(project).updateRepository(d.getGitRoot())
    } else {
        error("Git reset failed")
    }
}

inline fun edtExecute(crossinline body: () -> Unit) {
    ApplicationManager.getApplication().invokeAndWait {
        body()
    }
}

inline fun readAction(crossinline body: () -> Unit) {
    ApplicationManager.getApplication().runReadAction {
        body()
    }
}