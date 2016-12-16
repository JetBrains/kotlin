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

package org.jetbrains.kotlin.idea.configuration

import com.intellij.framework.FrameworkTypeEx
import com.intellij.framework.addSupport.FrameworkSupportInModuleProvider
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModifiableModelsProvider
import com.intellij.openapi.roots.ModifiableRootModel
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.idea.versions.bundledRuntimeVersion
import org.jetbrains.plugins.gradle.frameworkSupport.BuildScriptDataBuilder
import org.jetbrains.plugins.gradle.frameworkSupport.GradleFrameworkSupportProvider
import javax.swing.Icon

class GradleKotlinJavaFrameworkSupportProvider : GradleFrameworkSupportProvider() {
    override fun getFrameworkType(): FrameworkTypeEx = object : FrameworkTypeEx("KOTLIN") {
        override fun getIcon(): Icon = KotlinIcons.SMALL_LOGO

        override fun getPresentableName(): String = "Kotlin (Java)"

        override fun createProvider(): FrameworkSupportInModuleProvider = this@GradleKotlinJavaFrameworkSupportProvider
    }

    override fun addSupport(module: Module,
                            rootModel: ModifiableRootModel,
                            modifiableModelsProvider: ModifiableModelsProvider,
                            buildScriptData: BuildScriptDataBuilder) {
        var kotlinVersion = bundledRuntimeVersion()

        val additionalRepository: String? = when {
            kotlinVersion == "@snapshot@" -> {
                kotlinVersion = "1.1-SNAPSHOT"
                KotlinWithGradleConfigurator.SNAPSHOT_REPOSITORY_SNIPPET
            }
            is11Prerelease(kotlinVersion) -> {
                KotlinWithGradleConfigurator.EAP_11_REPOSITORY_SNIPPET
            }
            isEap(kotlinVersion) -> {
                KotlinWithGradleConfigurator.EAP_REPOSITORY_SNIPPET
            }
            else -> {
                null
            }
        }

        if (additionalRepository != null) {
            val oneLineRepository = additionalRepository.replace('\n', ' ')
            buildScriptData.addBuildscriptRepositoriesDefinition(oneLineRepository)

            buildScriptData.addRepositoriesDefinition("mavenCentral()")
            buildScriptData.addRepositoriesDefinition(oneLineRepository)
        }

        buildScriptData
                .addPluginDefinition(KotlinGradleModuleConfigurator.APPLY_KOTLIN)

                .addBuildscriptRepositoriesDefinition("mavenCentral()")
                .addRepositoriesDefinition("mavenCentral()")

                .addBuildscriptPropertyDefinition("ext.kotlin_version = '$kotlinVersion'")
                .addDependencyNotation(KotlinWithGradleConfigurator.LIBRARY)
                .addBuildscriptDependencyNotation(KotlinWithGradleConfigurator.CLASSPATH)
    }
}
