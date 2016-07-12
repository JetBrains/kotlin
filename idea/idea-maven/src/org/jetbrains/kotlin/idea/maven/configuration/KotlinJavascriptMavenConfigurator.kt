/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.maven.configuration

import com.intellij.openapi.module.Module
import org.jetbrains.idea.maven.dom.model.MavenDomPlugin
import org.jetbrains.kotlin.idea.configuration.hasKotlinJsRuntimeInScope
import org.jetbrains.kotlin.idea.maven.PomFile
import org.jetbrains.kotlin.js.resolve.JsPlatform
import org.jetbrains.kotlin.resolve.TargetPlatform

class KotlinJavascriptMavenConfigurator : KotlinMavenConfigurator(KotlinJavascriptMavenConfigurator.STD_LIB_ID, null, false, KotlinJavascriptMavenConfigurator.NAME, KotlinJavascriptMavenConfigurator.PRESENTABLE_TEXT) {

    override fun isKotlinModule(module: Module): Boolean {
        return hasKotlinJsRuntimeInScope(module)
    }

    override fun isRelevantGoal(goalName: String): Boolean {
        return goalName == PomFile.KotlinGoals.Js
    }

    override fun createExecutions(pomFile: PomFile, kotlinPlugin: MavenDomPlugin, module: Module) {
        createExecution(pomFile, kotlinPlugin, PomFile.DefaultPhases.Compile, PomFile.KotlinGoals.Js, module, false)
        createExecution(pomFile, kotlinPlugin, PomFile.DefaultPhases.TestCompile, PomFile.KotlinGoals.TestJs, module, true)
    }

    override fun getTargetPlatform(): TargetPlatform {
        return JsPlatform
    }

    companion object {
        private val NAME = "js maven"
        val STD_LIB_ID = "kotlin-js-library"
        private val PRESENTABLE_TEXT = "JavaScript Maven - experimental"
    }
}
