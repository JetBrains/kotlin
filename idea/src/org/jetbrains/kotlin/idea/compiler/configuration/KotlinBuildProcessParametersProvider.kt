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

package org.jetbrains.kotlin.idea.compiler.configuration

import com.intellij.compiler.server.BuildProcessParametersProvider
import org.jetbrains.kotlin.idea.PluginStartupComponent

class KotlinBuildProcessParametersProvider(private val compilerWorkspaceSettings: KotlinCompilerWorkspaceSettings,
                                                  private val kotlinPluginStartupComponent: PluginStartupComponent
): BuildProcessParametersProvider() {
    override fun getVMArguments(): MutableList<String> {
        val res = arrayListOf<String>()
        if (compilerWorkspaceSettings.preciseIncrementalEnabled) {
            res.add("-Dkotlin.incremental.compilation=true")
        }
        if (compilerWorkspaceSettings.enableDaemon) {
            res.add("-Dkotlin.daemon.enabled")
        }
        kotlinPluginStartupComponent.aliveFlagPath.let {
            if (!it.isBlank()) {
                // TODO: consider taking the property name from compiler/daemon/common (check whether dependency will be not too heavy)
                res.add("-Dkotlin.daemon.client.alive.path=\"$it\"")
            }
        }
        return res
    }
}
