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
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.idea.versions.*
import org.jetbrains.plugins.gradle.frameworkSupport.BuildScriptDataBuilder
import org.jetbrains.plugins.gradle.frameworkSupport.GradleFrameworkSupportProvider
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JLabel

abstract class GradleKotlinFrameworkSupportProvider(
    val frameworkTypeId: String,
    val displayName: String,
    val frameworkIcon: Icon
) : GradleFrameworkSupportProvider() {
    override fun getFrameworkType(): FrameworkTypeEx = object : FrameworkTypeEx(frameworkTypeId) {
        override fun getIcon(): Icon = frameworkIcon

        override fun getPresentableName(): String = displayName

        override fun createProvider(): FrameworkSupportInModuleProvider = this@GradleKotlinFrameworkSupportProvider
    }

    override fun createConfigurable(model: FrameworkSupportModel): FrameworkSupportInModuleConfigurable {
        val configurable = KotlinGradleFrameworkSupportInModuleConfigurable(model, this)
        return object : FrameworkSupportInModuleConfigurable() {
            override fun addSupport(module: Module, rootModel: ModifiableRootModel, modifiableModelsProvider: ModifiableModelsProvider) {
                configurable.addSupport(module, rootModel, modifiableModelsProvider)
            }

            override fun createComponent(): JComponent = JLabel(getDescription())
        }
    }

    override fun addSupport(
        module: Module,
        rootModel: ModifiableRootModel,
        modifiableModelsProvider: ModifiableModelsProvider,
        buildScriptData: BuildScriptDataBuilder
    ) {
        addSupport(buildScriptData, module, rootModel.sdk, true)
    }

    open fun addSupport(buildScriptData: BuildScriptDataBuilder, module: Module, sdk: Sdk?, specifyPluginVersionIfNeeded: Boolean) {
        var kotlinVersion = bundledRuntimeVersion()
        val additionalRepository = getRepositoryForVersion(kotlinVersion)
        if (isSnapshot(bundledRuntimeVersion())) {
            kotlinVersion = LAST_SNAPSHOT_VERSION
        }

        val useNewSyntax = buildScriptData.gradleVersion >= MIN_GRADLE_VERSION_FOR_NEW_PLUGIN_SYNTAX
        if (useNewSyntax) {
            if (additionalRepository != null) {
                val oneLineRepository = additionalRepository.toGroovyRepositorySnippet().replace('\n', ' ')
                updateSettingsScript(module) {
                    with(KotlinWithGradleConfigurator.getManipulator(it)) {
                        addPluginRepository(additionalRepository)
                        addMavenCentralPluginRepository()
                    }
                }
                buildScriptData.addRepositoriesDefinition("mavenCentral()")
                buildScriptData.addRepositoriesDefinition(oneLineRepository)
            }

            buildScriptData.addPluginDefinitionInPluginsGroup(
                    getPluginExpression() + if (specifyPluginVersionIfNeeded) " version '$kotlinVersion'" else ""
            )
        } else {
            if (additionalRepository != null) {
                val oneLineRepository = additionalRepository.toGroovyRepositorySnippet().replace('\n', ' ')
                buildScriptData.addBuildscriptRepositoriesDefinition(oneLineRepository)

                buildScriptData.addRepositoriesDefinition("mavenCentral()")
                buildScriptData.addRepositoriesDefinition(oneLineRepository)
            }

            buildScriptData
                .addPluginDefinition(KotlinWithGradleConfigurator.getGroovyApplyPluginDirective(getPluginId()))
                .addBuildscriptRepositoriesDefinition("mavenCentral()")
                .addBuildscriptPropertyDefinition("ext.kotlin_version = '$kotlinVersion'")
        }

        buildScriptData.addRepositoriesDefinition("mavenCentral()")

        for (dependency in getDependencies(sdk)) {
            buildScriptData.addDependencyNotation(KotlinWithGradleConfigurator.getGroovyDependencySnippet(dependency, "compile", !useNewSyntax))
        }
        for (dependency in getTestDependencies()) {
            buildScriptData.addDependencyNotation(
                if (":" in dependency)
                    "testCompile \"$dependency\""
                else
                    KotlinWithGradleConfigurator.getGroovyDependencySnippet(dependency, "testCompile", !useNewSyntax)
            )
        }

        if (useNewSyntax) {
            updateSettingsScript(module) { updateSettingsScript(it, specifyPluginVersionIfNeeded) }
        } else {
            buildScriptData.addBuildscriptDependencyNotation(KotlinWithGradleConfigurator.CLASSPATH)
        }
    }

    protected open fun updateSettingsScript(settingsScript: PsiFile, specifyPluginVersionIfNeeded: Boolean) { }

    protected abstract fun getDependencies(sdk: Sdk?): List<String>
    protected open fun getTestDependencies(): List<String> = listOf()

    protected abstract fun getPluginId(): String
    protected abstract fun getPluginExpression(): String

    protected abstract fun getDescription(): String
}

open class GradleKotlinJavaFrameworkSupportProvider(
    frameworkTypeId: String = "KOTLIN",
    displayName: String = "Kotlin (Java)"
) : GradleKotlinFrameworkSupportProvider(frameworkTypeId, displayName, KotlinIcons.SMALL_LOGO) {

    override fun getPluginId() = KotlinGradleModuleConfigurator.KOTLIN
    override fun getPluginExpression() = "id 'org.jetbrains.kotlin.jvm'"

    override fun getDependencies(sdk: Sdk?) = listOf(getStdlibArtifactId(sdk, bundledRuntimeVersion()))

    override fun addSupport(buildScriptData: BuildScriptDataBuilder, module: Module, sdk: Sdk?, specifyPluginVersionIfNeeded: Boolean) {
        super.addSupport(buildScriptData, module, sdk, specifyPluginVersionIfNeeded)
        val jvmTarget = getDefaultJvmTarget(sdk, bundledRuntimeVersion())
        if (jvmTarget != null) {
            val description = jvmTarget.description
            buildScriptData.addOther("compileKotlin {\n    kotlinOptions.jvmTarget = \"$description\"\n}\n\n")
            buildScriptData.addOther("compileTestKotlin {\n    kotlinOptions.jvmTarget = \"$description\"\n}\n")
        }
    }

    override fun getDescription() = "A Kotlin library or application targeting the JVM"
}

open class GradleKotlinJSFrameworkSupportProvider(
    frameworkTypeId: String = "KOTLIN_JS",
    displayName: String = "Kotlin (JavaScript)"
) : GradleKotlinFrameworkSupportProvider(frameworkTypeId, displayName, KotlinIcons.JS) {

    override fun getPluginId() = KotlinJsGradleModuleConfigurator.KOTLIN_JS
    override fun getPluginExpression() = "id 'kotlin2js'"

    override fun getDependencies(sdk: Sdk?) = listOf(MAVEN_JS_STDLIB_ID)

    override fun getTestDependencies() = listOf(MAVEN_JS_TEST_ID)

    override fun updateSettingsScript(settingsScript: PsiFile, specifyPluginVersionIfNeeded: Boolean) {
        if (specifyPluginVersionIfNeeded) {
            KotlinWithGradleConfigurator.getManipulator(settingsScript).addResolutionStrategy("kotlin2js")
        }
    }

    override fun getDescription() = "A Kotlin library or application targeting JavaScript"
}

open class GradleKotlinMPPCommonFrameworkSupportProvider :
    GradleKotlinFrameworkSupportProvider("KOTLIN_MPP_COMMON", "Kotlin (Multiplatform Common - Experimental)", KotlinIcons.MPP) {
    override fun getPluginId() = "kotlin-platform-common"
    override fun getPluginExpression() = "id 'kotlin-platform-common'"

    override fun getDependencies(sdk: Sdk?) = listOf(MAVEN_COMMON_STDLIB_ID)
    override fun getTestDependencies() = listOf(MAVEN_COMMON_TEST_ID, MAVEN_COMMON_TEST_ANNOTATIONS_ID)

    override fun updateSettingsScript(settingsScript: PsiFile, specifyPluginVersionIfNeeded: Boolean) {
        if (specifyPluginVersionIfNeeded) {
            KotlinWithGradleConfigurator.getManipulator(settingsScript).addResolutionStrategy("kotlin-platform-common")
        }
    }

    override fun getDescription() = "Shared code for a Kotlin multiplatform project (targeting JVM and JS)"
}

class GradleKotlinMPPJavaFrameworkSupportProvider
    : GradleKotlinJavaFrameworkSupportProvider("KOTLIN_MPP_JVM", "Kotlin (Multiplatform JVM - Experimental)") {

    override fun getPluginId() = "kotlin-platform-jvm"
    override fun getPluginExpression() = "id 'kotlin-platform-jvm'"

    override fun getDescription() = "JVM-specific code for a Kotlin multiplatform project"
    override fun getTestDependencies() = listOf(MAVEN_TEST_ID, MAVEN_TEST_JUNIT_ID, "junit:junit:4.12")

    override fun addSupport(buildScriptData: BuildScriptDataBuilder, module: Module, sdk: Sdk?, specifyPluginVersionIfNeeded: Boolean) {
        super.addSupport(buildScriptData, module, sdk, specifyPluginVersionIfNeeded)
        val jvmTarget = getDefaultJvmTarget(sdk, bundledRuntimeVersion())
        if (jvmTarget != null) {
            val description = jvmTarget.description
            buildScriptData.addOther("sourceCompatibility = \"$description\"\n\n")
        }
    }

    override fun updateSettingsScript(settingsScript: PsiFile, specifyPluginVersionIfNeeded: Boolean) {
        if (specifyPluginVersionIfNeeded) {
            KotlinWithGradleConfigurator.getManipulator(settingsScript).addResolutionStrategy("kotlin-platform-jvm")
        }
    }
}

class GradleKotlinMPPJSFrameworkSupportProvider
    : GradleKotlinJSFrameworkSupportProvider("KOTLIN_MPP_JS", "Kotlin (Multiplatform JS - Experimental)") {

    override fun getPluginId() = "kotlin-platform-js"
    override fun getPluginExpression() = "id 'kotlin-platform-js'"

    override fun updateSettingsScript(settingsScript: PsiFile, specifyPluginVersionIfNeeded: Boolean) {
        if (specifyPluginVersionIfNeeded) {
            KotlinWithGradleConfigurator.getManipulator(settingsScript).addResolutionStrategy("kotlin-platform-js")
        }
    }

    override fun getDescription() = "JavaScript-specific code for a Kotlin multiplatform project"
}
