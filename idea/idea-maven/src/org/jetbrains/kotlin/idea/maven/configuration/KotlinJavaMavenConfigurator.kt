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
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.psi.PsiFile
import org.jetbrains.idea.maven.dom.model.MavenDomPlugin
import org.jetbrains.kotlin.idea.configuration.NotificationMessageCollector
import org.jetbrains.kotlin.idea.configuration.addStdlibToJavaModuleInfo
import org.jetbrains.kotlin.idea.configuration.hasKotlinJvmRuntimeInScope
import org.jetbrains.kotlin.idea.maven.PomFile
import org.jetbrains.kotlin.idea.versions.getDefaultJvmTarget
import org.jetbrains.kotlin.idea.versions.getStdlibArtifactId
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform

class KotlinJavaMavenConfigurator : KotlinMavenConfigurator(
    KotlinJavaMavenConfigurator.TEST_LIB_ID,
    false,
    KotlinJavaMavenConfigurator.NAME,
    KotlinJavaMavenConfigurator.PRESENTABLE_TEXT
) {

    override fun isKotlinModule(module: Module) =
        hasKotlinJvmRuntimeInScope(module)

    override fun isRelevantGoal(goalName: String) =
        goalName == PomFile.KotlinGoals.Compile

    override fun getStdlibArtifactId(module: Module, version: String): String =
        getStdlibArtifactId(ModuleRootManager.getInstance(module).sdk, version)

    override fun createExecutions(pomFile: PomFile, kotlinPlugin: MavenDomPlugin, module: Module) {
        createExecution(pomFile, kotlinPlugin, PomFile.DefaultPhases.Compile, PomFile.KotlinGoals.Compile, module, false)
        createExecution(pomFile, kotlinPlugin, PomFile.DefaultPhases.TestCompile, PomFile.KotlinGoals.TestCompile, module, true)
    }

    override fun configurePlugin(pom: PomFile, plugin: MavenDomPlugin, module: Module, version: String) {
        val sdk = ModuleRootManager.getInstance(module).sdk
        val jvmTarget = getDefaultJvmTarget(sdk, version)
        if (jvmTarget != null) {
            pom.addPluginConfiguration(plugin, "jvmTarget", jvmTarget.description)
        }
    }

    override fun configureModule(module: Module, file: PsiFile, version: String, collector: NotificationMessageCollector): Boolean {
        if (!super.configureModule(module, file, version, collector)) {
            return false
        }

        addStdlibToJavaModuleInfo(module, collector)
        return true
    }

    override val targetPlatform: TargetPlatform
        get() = JvmPlatform

    companion object {
        private const val NAME = "maven"
        const val TEST_LIB_ID = "kotlin-test"
        private const val PRESENTABLE_TEXT = "Java with Maven"
    }
}
