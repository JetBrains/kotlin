/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
import com.intellij.openapi.externalSystem.model.project.ProjectId
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModifiableModelsProvider
import com.intellij.openapi.roots.ModifiableRootModel
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.idea.configuration.KotlinBuildScriptManipulator.Companion.GSK_KOTLIN_VERSION_PROPERTY_NAME
import org.jetbrains.kotlin.idea.configuration.KotlinBuildScriptManipulator.Companion.getCompileDependencySnippet
import org.jetbrains.kotlin.idea.configuration.KotlinBuildScriptManipulator.Companion.getKotlinGradlePluginClassPathSnippet
import org.jetbrains.kotlin.idea.versions.*
import org.jetbrains.plugins.gradle.frameworkSupport.BuildScriptDataBuilder
import org.jetbrains.plugins.gradle.frameworkSupport.KotlinDslGradleFrameworkSupportProvider
import javax.swing.Icon

abstract class KotlinDslGradleKotlinFrameworkSupportProvider(
        val frameworkTypeId: String,
        val displayName: String,
        val frameworkIcon: Icon
) : KotlinDslGradleFrameworkSupportProvider() {
    override fun getFrameworkType(): FrameworkTypeEx = object : FrameworkTypeEx(frameworkTypeId) {
        override fun getIcon(): Icon = frameworkIcon
        override fun getPresentableName(): String = displayName
        override fun createProvider(): FrameworkSupportInModuleProvider = this@KotlinDslGradleKotlinFrameworkSupportProvider
    }

    override fun addSupport(projectId: ProjectId,
                            module: Module,
                            rootModel: ModifiableRootModel,
                            modifiableModelsProvider: ModifiableModelsProvider,
                            buildScriptData: BuildScriptDataBuilder) {
        var kotlinVersion = bundledRuntimeVersion()
        val additionalRepository = getRepositoryForVersion(kotlinVersion)
        if (isSnapshot(bundledRuntimeVersion())) {
            kotlinVersion = "1.1-SNAPSHOT"
        }

        if (additionalRepository != null) {
            val repository = additionalRepository.toKotlinRepositorySnippet()
            buildScriptData.addBuildscriptRepositoriesDefinition(repository)
            buildScriptData.addRepositoriesDefinition("mavenCentral()")
            buildScriptData.addRepositoriesDefinition(repository)
        }

        buildScriptData
                .addPropertyDefinition("val $GSK_KOTLIN_VERSION_PROPERTY_NAME: String by extra")
                .addPluginDefinition(getPluginDefinition())
                .addBuildscriptRepositoriesDefinition("mavenCentral()")
                .addRepositoriesDefinition("mavenCentral()")
                // TODO: in gradle > 4.1 this could be single declaration e.g. 'val kotlin_version: String by extra { "1.1.11" }'
                .addBuildscriptPropertyDefinition("var $GSK_KOTLIN_VERSION_PROPERTY_NAME: String by extra\n$GSK_KOTLIN_VERSION_PROPERTY_NAME = \"$kotlinVersion\"")
                .addDependencyNotation(getRuntimeLibrary(rootModel))
                .addBuildscriptDependencyNotation(getKotlinGradlePluginClassPathSnippet())
    }

    protected abstract fun getRuntimeLibrary(rootModel: ModifiableRootModel): String

    protected abstract fun getPluginDefinition(): String
}

class KotlinDslGradleKotlinJavaFrameworkSupportProvider :
        KotlinDslGradleKotlinFrameworkSupportProvider("KOTLIN", "Kotlin (Java)", KotlinIcons.SMALL_LOGO) {

    override fun getPluginDefinition() = "plugin(\"${KotlinGradleModuleConfigurator.KOTLIN}\")"

    override fun getRuntimeLibrary(rootModel: ModifiableRootModel) =
            getCompileDependencySnippet(KOTLIN_GROUP_ID, getStdlibArtifactId(rootModel.sdk, bundledRuntimeVersion()))

    override fun addSupport(
            projectId: ProjectId,
            module: Module,
            rootModel: ModifiableRootModel,
            modifiableModelsProvider: ModifiableModelsProvider,
            buildScriptData: BuildScriptDataBuilder
    ) {
        super.addSupport(projectId, module, rootModel, modifiableModelsProvider, buildScriptData)
        val jvmTarget = getDefaultJvmTarget(rootModel.sdk, bundledRuntimeVersion())
        if (jvmTarget != null) {
            buildScriptData
                    .addImport("import org.jetbrains.kotlin.gradle.tasks.KotlinCompile")
                    .addOther("tasks.withType<KotlinCompile> {\n    kotlinOptions.jvmTarget = \"1.8\"\n}\n")
        }
    }
}

class KotlinDslGradleKotlinJSFrameworkSupportProvider :
        KotlinDslGradleKotlinFrameworkSupportProvider("KOTLIN_JS", "Kotlin (JavaScript)", KotlinIcons.JS) {

    override fun getPluginDefinition(): String = "plugin(\"${KotlinJsGradleModuleConfigurator.KOTLIN_JS}\")"

    override fun getRuntimeLibrary(rootModel: ModifiableRootModel) =
            getCompileDependencySnippet(KOTLIN_GROUP_ID, MAVEN_JS_STDLIB_ID.removePrefix("kotlin-"))
}
