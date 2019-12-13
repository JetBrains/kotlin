/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.console

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.util.JavaParametersBuilder
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import java.util.concurrent.ConcurrentHashMap

private const val REPL_TITLE = "Kotlin REPL"

class KotlinConsoleKeeper(val project: Project) {
    private val consoleMap: MutableMap<VirtualFile, KotlinConsoleRunner> = ConcurrentHashMap()

    fun getConsoleByVirtualFile(virtualFile: VirtualFile) = consoleMap[virtualFile]
    fun putVirtualFileToConsole(virtualFile: VirtualFile, console: KotlinConsoleRunner) = consoleMap.put(virtualFile, console)
    fun removeConsole(virtualFile: VirtualFile) = consoleMap.remove(virtualFile)

    fun run(module: Module, previousCompilationFailed: Boolean = false): KotlinConsoleRunner? {
        val path = module.moduleFilePath
        val cmdLine = createReplCommandLine(project, module)

        val consoleRunner = KotlinConsoleRunner(module, cmdLine, previousCompilationFailed, project, REPL_TITLE, path)
        consoleRunner.initAndRun()
        return consoleRunner
    }

    companion object {
        @JvmStatic
        fun getInstance(project: Project) = ServiceManager.getService(project, KotlinConsoleKeeper::class.java)

        fun createReplCommandLine(project: Project, module: Module?): GeneralCommandLine {
            val javaParameters = JavaParametersBuilder(project)
                .withSdkFrom(module, true)
                .withMainClassName("org.jetbrains.kotlin.cli.jvm.K2JVMCompiler")
                .build()

            javaParameters.charset = null
            javaParameters.vmParametersList.add("-Dkotlin.repl.ideMode=true")

            javaParameters.classPath.apply {
                val kotlinPaths = PathUtil.kotlinPathsForIdeaPlugin
                addAll(kotlinPaths.compilerClasspath.map { it.absolutePath })
                add(kotlinPaths.compilerPath.absolutePath)
            }

            if (module != null) {
                val classPath = JavaParametersBuilder.getModuleDependencies(module)
                if (classPath.isNotEmpty()) {
                    javaParameters.setUseDynamicParameters(javaParameters.isDynamicClasspath)
                    javaParameters.programParametersList.add("-cp")
                    javaParameters.programParametersList.add(
                        classPath.joinToString(File.pathSeparator)
                    )
                }
            }

            return javaParameters.toCommandLine()
        }
    }
}
