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

package org.jetbrains.kotlin.gradle.kdsl.frameworkSupport

import com.intellij.framework.FrameworkTypeEx
import com.intellij.framework.addSupport.FrameworkSupportInModuleProvider
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModifiableModelsProvider
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.vfs.VfsUtil
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.idea.configuration.*
import org.jetbrains.kotlin.idea.configuration.KotlinBuildScriptManipulator.Companion.GSK_KOTLIN_VERSION_PROPERTY_NAME
import org.jetbrains.kotlin.idea.configuration.KotlinBuildScriptManipulator.Companion.getCompileDependencySnippet
import org.jetbrains.kotlin.idea.configuration.KotlinBuildScriptManipulator.Companion.getKotlinGradlePluginClassPathSnippet
import org.jetbrains.kotlin.idea.versions.MAVEN_JS_STDLIB_ID
import org.jetbrains.kotlin.idea.versions.bundledRuntimeVersion
import org.jetbrains.kotlin.idea.versions.getDefaultJvmTarget
import org.jetbrains.kotlin.idea.versions.getStdlibArtifactId
import javax.swing.Icon

abstract class GradleKotlinDSLKotlinFrameworkSupportProvider(
        val frameworkTypeId: String,
        val displayName: String,
        val frameworkIcon: Icon
) : GradleFrameworkSupportProvider() {
    override fun getFrameworkType(): FrameworkTypeEx = object : FrameworkTypeEx(frameworkTypeId) {
        override fun getIcon(): Icon = frameworkIcon
        override fun getPresentableName(): String = displayName
        override fun createProvider(): FrameworkSupportInModuleProvider = this@GradleKotlinDSLKotlinFrameworkSupportProvider
    }

    override fun addSupport(module: Module,
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

class GradleKotlinDSLKotlinJavaFrameworkSupportProvider :
        GradleKotlinDSLKotlinFrameworkSupportProvider("KOTLIN", "Kotlin (Java)", KotlinIcons.SMALL_LOGO) {

    override fun getPluginDefinition() = "plugin(\"${KotlinGradleModuleConfigurator.KOTLIN}\")"

    override fun getRuntimeLibrary(rootModel: ModifiableRootModel) =
            getCompileDependencySnippet(KOTLIN_GROUP_ID, getStdlibArtifactId(rootModel.sdk, bundledRuntimeVersion()))

    override fun addSupport(
            module: Module,
            rootModel: ModifiableRootModel,
            modifiableModelsProvider: ModifiableModelsProvider,
            buildScriptData: BuildScriptDataBuilder
    ) {
        super.addSupport(module, rootModel, modifiableModelsProvider, buildScriptData)
        val jvmTarget = getDefaultJvmTarget(rootModel.sdk, bundledRuntimeVersion())
        if (jvmTarget != null) {
            buildScriptData
                    .addImports("import org.jetbrains.kotlin.gradle.tasks.KotlinCompile")
                    .addOther("tasks.withType<KotlinCompile> {\n    kotlinOptions.jvmTarget = \"1.8\"\n}\n")
        }
    }

    private fun BuildScriptDataBuilder.addImports(vararg import: String): BuildScriptDataBuilder = apply {
        val text = VfsUtil.loadText(buildScriptFile)
        VfsUtil.saveText(buildScriptFile, import.joinToString(separator = "\n") + "\n\n" + text)
    }
}

class GradleKotlinDSLKotlinJSFrameworkSupportProvider :
        GradleKotlinDSLKotlinFrameworkSupportProvider("KOTLIN_JS", "Kotlin (JavaScript)", KotlinIcons.JS) {

    override fun getPluginDefinition(): String = "plugin(\"${KotlinJsGradleModuleConfigurator.KOTLIN_JS}\")"

    override fun getRuntimeLibrary(rootModel: ModifiableRootModel) =
            getCompileDependencySnippet(KOTLIN_GROUP_ID, MAVEN_JS_STDLIB_ID.removePrefix("kotlin-"))
}
