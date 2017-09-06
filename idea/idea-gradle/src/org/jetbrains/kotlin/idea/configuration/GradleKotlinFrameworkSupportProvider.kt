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
import com.intellij.framework.addSupport.FrameworkSupportInModuleConfigurable
import com.intellij.framework.addSupport.FrameworkSupportInModuleProvider
import com.intellij.ide.util.frameworkSupport.FrameworkSupportModel
import com.intellij.openapi.module.Module
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ModifiableModelsProvider
import com.intellij.openapi.roots.ModifiableRootModel
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.idea.versions.*
import org.jetbrains.plugins.gradle.frameworkSupport.BuildScriptDataBuilder
import org.jetbrains.plugins.gradle.frameworkSupport.GradleFrameworkSupportProvider
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JLabel

abstract class GradleKotlinFrameworkSupportProvider(val frameworkTypeId: String,
                                                    val displayName: String,
                                                    val frameworkIcon: Icon) : GradleFrameworkSupportProvider() {
    override fun getFrameworkType(): FrameworkTypeEx = object : FrameworkTypeEx(frameworkTypeId) {
        override fun getIcon(): Icon = frameworkIcon

        override fun getPresentableName(): String = displayName

        override fun createProvider(): FrameworkSupportInModuleProvider = this@GradleKotlinFrameworkSupportProvider
    }

    override fun createConfigurable(model: FrameworkSupportModel): FrameworkSupportInModuleConfigurable {
        val configurable = super.createConfigurable(model)
        return object : FrameworkSupportInModuleConfigurable() {
            override fun addSupport(module: Module, rootModel: ModifiableRootModel, modifiableModelsProvider: ModifiableModelsProvider) {
                configurable.addSupport(module, rootModel, modifiableModelsProvider)
            }

            override fun createComponent(): JComponent = JLabel(getDescription())
        }
    }

    override fun addSupport(module: Module,
                            rootModel: ModifiableRootModel,
                            modifiableModelsProvider: ModifiableModelsProvider,
                            buildScriptData: BuildScriptDataBuilder) {
        addSupport(buildScriptData, rootModel.sdk)
    }

    fun addSupport(buildScriptData: BuildScriptDataBuilder, sdk: Sdk?) {
        var kotlinVersion = bundledRuntimeVersion()
        val additionalRepository = getRepositoryForVersion(kotlinVersion)
        if (isSnapshot(bundledRuntimeVersion())) {
            kotlinVersion = "1.1-SNAPSHOT"
        }

        if (additionalRepository != null) {
            val oneLineRepository = additionalRepository.toGroovyRepositorySnippet().replace('\n', ' ')
            buildScriptData.addBuildscriptRepositoriesDefinition(oneLineRepository)

            buildScriptData.addRepositoriesDefinition("mavenCentral()")
            buildScriptData.addRepositoriesDefinition(oneLineRepository)
        }

        buildScriptData
            .addPluginDefinition(KotlinWithGradleConfigurator.getGroovyApplyPluginDirective(getPluginId()))

            .addBuildscriptRepositoriesDefinition("mavenCentral()")
            .addRepositoriesDefinition("mavenCentral()")

            .addBuildscriptPropertyDefinition("ext.kotlin_version = '$kotlinVersion'")
            .addDependencyNotation(getRuntimeLibrary(sdk))
            .addBuildscriptDependencyNotation(KotlinWithGradleConfigurator.CLASSPATH)
    }

    protected abstract fun getRuntimeLibrary(sdk: Sdk?): String

    protected abstract fun getPluginId(): String

    protected abstract fun getDescription(): String
}

open class GradleKotlinJavaFrameworkSupportProvider(frameworkTypeId: String = "KOTLIN",
                                                    displayName: String = "Kotlin (Java)")
    : GradleKotlinFrameworkSupportProvider(frameworkTypeId, displayName, KotlinIcons.SMALL_LOGO) {

    override fun getPluginId() = KotlinGradleModuleConfigurator.KOTLIN

    override fun getRuntimeLibrary(sdk: Sdk?) =
            KotlinWithGradleConfigurator.getGroovyDependencySnippet(getStdlibArtifactId(sdk, bundledRuntimeVersion()))

    override fun addSupport(module: Module, rootModel: ModifiableRootModel, modifiableModelsProvider: ModifiableModelsProvider, buildScriptData: BuildScriptDataBuilder) {
        super.addSupport(module, rootModel, modifiableModelsProvider, buildScriptData)
        val jvmTarget = getDefaultJvmTarget(rootModel.sdk, bundledRuntimeVersion())
        if (jvmTarget != null) {
            buildScriptData.addOther("compileKotlin {\n    kotlinOptions.jvmTarget = \"1.8\"\n}\n\n")
            buildScriptData.addOther("compileTestKotlin {\n    kotlinOptions.jvmTarget = \"1.8\"\n}\n")
        }
    }

    override fun getDescription() = "A Kotlin library or application targeting the JVM"
}

open class GradleKotlinJSFrameworkSupportProvider(frameworkTypeId: String = "KOTLIN_JS",
                                                  displayName: String = "Kotlin (JavaScript)")
    : GradleKotlinFrameworkSupportProvider(frameworkTypeId, displayName, KotlinIcons.JS) {

    override fun getPluginId() = KotlinJsGradleModuleConfigurator.KOTLIN_JS

    override fun getRuntimeLibrary(sdk: Sdk?) =
            KotlinWithGradleConfigurator.getGroovyDependencySnippet(MAVEN_JS_STDLIB_ID)

    override fun getDescription() = "A Kotlin library or application targeting JavaScript"
}

open class GradleKotlinMPPCommonFrameworkSupportProvider :
        GradleKotlinFrameworkSupportProvider("KOTLIN_MPP_COMMON", "Kotlin (Multiplatform - Common)", KotlinIcons.SMALL_LOGO) {
    override fun getPluginId() = "kotlin-platform-common"

    override fun getRuntimeLibrary(sdk: Sdk?) =
            KotlinWithGradleConfigurator.getGroovyDependencySnippet(MAVEN_COMMON_STDLIB_ID)

    override fun getDescription() = "Shared code for a Kotlin multiplatform project (targeting JVM and JS)"
}

class GradleKotlinMPPJavaFrameworkSupportProvider
    : GradleKotlinJavaFrameworkSupportProvider("KOTLIN_MPP_JVM", "Kotlin (Multiplatform - JVM)") {

    override fun getPluginId() = "kotlin-platform-jvm"
    override fun getDescription() = "JVM-specific code for a Kotlin multiplatform project"
}

class GradleKotlinMPPJSFrameworkSupportProvider
    : GradleKotlinJSFrameworkSupportProvider("KOTLIN_MPP_JS", "Kotlin (Multiplatform - JS)") {

    override fun getPluginId() = "kotlin-platform-js"
    override fun getDescription() = "JavaScript-specific code for a Kotlin multiplatform project"
}
