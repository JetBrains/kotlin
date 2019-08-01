/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.compiler.configuration

import com.intellij.compiler.server.BuildProcessParametersProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.idea.PluginStartupComponent

class KotlinBuildProcessParametersProvider(private val project: Project) : BuildProcessParametersProvider() {
    override fun getVMArguments(): MutableList<String> {
        val compilerWorkspaceSettings = KotlinCompilerWorkspaceSettings.getInstance(project)

        val res = arrayListOf<String>()
        if (compilerWorkspaceSettings.preciseIncrementalEnabled) {
            res.add("-D" + IncrementalCompilation.INCREMENTAL_COMPILATION_JVM_PROPERTY + "=true")
        }
        if (compilerWorkspaceSettings.incrementalCompilationForJsEnabled) {
            res.add("-D" + IncrementalCompilation.INCREMENTAL_COMPILATION_JS_PROPERTY + "=true")
        }
        if (compilerWorkspaceSettings.enableDaemon) {
            res.add("-Dkotlin.daemon.enabled")
        }
        if (Registry.`is`("kotlin.jps.instrument.bytecode", false)) {
            res.add("-Dkotlin.jps.instrument.bytecode=true")
        }
        PluginStartupComponent.getInstance().aliveFlagPath.let {
            if (!it.isBlank()) {
                // TODO: consider taking the property name from compiler/daemon/common (check whether dependency will be not too heavy)
                res.add("-Dkotlin.daemon.client.alive.path=\"$it\"")
            }
        }
        return res
    }
}
