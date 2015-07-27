/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.console

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.JavaParameters
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdkType
import com.intellij.openapi.projectRoots.JdkUtil
import com.intellij.openapi.projectRoots.SimpleJavaSdkType
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.SystemProperties
import org.jetbrains.kotlin.console.actions.errorNotification
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.platform.platformStatic

public class KotlinConsoleKeeper(val project: Project) {
    private val consoleMap: MutableMap<VirtualFile, KotlinConsoleRunner> = ConcurrentHashMap()

    fun getConsoleByVirtualFile(virtualFile: VirtualFile) = consoleMap.get(virtualFile)
    fun putVirtualFileToConsole(virtualFile: VirtualFile, console: KotlinConsoleRunner) = consoleMap.put(virtualFile, console)
    fun removeConsole(virtualFile: VirtualFile?) = consoleMap.remove(virtualFile)

    fun run(module: Module): KotlinConsoleRunner? {
        val path = module.moduleFilePath
        val cmdLine = createCommandLine(module)
        if (cmdLine == null) {
            errorNotification(project, "<p>Module SDK not found</p>")
            return null
        }

        val consoleRunner = KotlinConsoleRunner(path, cmdLine, project, REPL_TITLE)
        consoleRunner.initAndRun()
        consoleRunner.setupGutters()

        return consoleRunner
    }

    private fun createCommandLine(module: Module): GeneralCommandLine? {
        val javaParameters = createJavaParametersWithSdk(module)
        val sdk = javaParameters.jdk ?: return null
        val sdkType = sdk.sdkType
        val exePath = (sdkType as JavaSdkType).getVMExecutablePath(sdk)

        val commandLine = JdkUtil.setupJVMCommandLine(exePath, javaParameters, true)

        // set parameters to run compiler
        val paramList = commandLine.parametersList
        paramList.clearAll()

        // use to debug repl process
        //paramList.add("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005")

        // set classpath for process to compiler
        paramList.add("-cp")
        paramList.add("${PathUtil.getKotlinPathsForIdeaPlugin().libPath.absolutePath}/*")

        // set param to prevent process from repeating input backwards
        paramList.add("-Drepl.ideMode=true")

        // path to compiler and his options
        paramList.add("org.jetbrains.kotlin.cli.jvm.K2JVMCompiler")

        return commandLine
    }

    private fun createJavaParametersWithSdk(module: Module?): JavaParameters {
        val params = JavaParameters()
        params.charset = null

        if (module != null) {
            val sdk = ModuleRootManager.getInstance(module).sdk
            if (sdk != null && sdk.sdkType is JavaSdkType && File(sdk.homePath).exists()) {
                params.jdk = sdk
            }
        }
        if (params.jdk == null) {
            params.jdk = SimpleJavaSdkType().createJdk("tmp", SystemProperties.getJavaHome())
        }

        return params
    }

    companion object {
        private val REPL_TITLE = "Kotlin REPL"

        platformStatic fun getInstance(project: Project) = ServiceManager.getService(project, javaClass<KotlinConsoleKeeper>())
    }
}