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
import com.intellij.ide.util.frameworkSupport.FrameworkSupportModel
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys
import com.intellij.openapi.externalSystem.model.project.ProjectId
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModifiableModelsProvider
import com.intellij.openapi.roots.ModifiableRootModel
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.idea.configuration.KotlinBuildScriptManipulator.Companion.GSK_KOTLIN_VERSION_PROPERTY_NAME
import org.jetbrains.kotlin.idea.configuration.KotlinBuildScriptManipulator.Companion.getKotlinGradlePluginClassPathSnippet
import org.jetbrains.kotlin.idea.configuration.KotlinBuildScriptManipulator.Companion.getKotlinModuleDependencySnippet
import org.jetbrains.kotlin.idea.formatter.KotlinStyleGuideCodeStyle
import org.jetbrains.kotlin.idea.formatter.ProjectCodeStyleImporter
import org.jetbrains.kotlin.idea.statistics.FUSEventGroups
import org.jetbrains.kotlin.idea.statistics.KotlinFUSLogger
import org.jetbrains.kotlin.idea.util.isSnapshot
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

    override fun createConfigurable(model: FrameworkSupportModel) = KotlinGradleFrameworkSupportInModuleConfigurable(model, this)

    override fun addSupport(
        projectId: ProjectId,
        module: Module,
        rootModel: ModifiableRootModel,
        modifiableModelsProvider: ModifiableModelsProvider,
        buildScriptData: BuildScriptDataBuilder
    ) {
        var kotlinVersion = bundledRuntimeVersion()
        val additionalRepository = getRepositoryForVersion(kotlinVersion)
        if (isSnapshot(bundledRuntimeVersion())) {
            kotlinVersion = LAST_SNAPSHOT_VERSION
        }

        val useNewSyntax = buildScriptData.gradleVersion >= MIN_GRADLE_VERSION_FOR_NEW_PLUGIN_SYNTAX
        if (useNewSyntax) {
            if (additionalRepository != null) {
                val repository = additionalRepository.toKotlinRepositorySnippet()
                updateSettingsScript(module) {
                    with(it) {
                        addPluginRepository(additionalRepository)
                        addMavenCentralPluginRepository()
                        addPluginRepository(DEFAULT_GRADLE_PLUGIN_REPOSITORY)
                    }
                }
                buildScriptData.addRepositoriesDefinition("mavenCentral()")
                buildScriptData.addRepositoriesDefinition(repository)
            }

            buildScriptData
                .addPluginDefinitionInPluginsGroup(getPluginDefinition() + " version \"$kotlinVersion\"")
        } else {
            if (additionalRepository != null) {
                val repository = additionalRepository.toKotlinRepositorySnippet()
                buildScriptData.addBuildscriptRepositoriesDefinition(repository)
                buildScriptData.addRepositoriesDefinition("mavenCentral()")
                buildScriptData.addRepositoriesDefinition(repository)
            }

            buildScriptData
                .addPropertyDefinition("val $GSK_KOTLIN_VERSION_PROPERTY_NAME: String by extra")
                .addPluginDefinition(getOldSyntaxPluginDefinition())
                .addBuildscriptRepositoriesDefinition("mavenCentral()")
                // TODO: in gradle > 4.1 this could be single declaration e.g. 'val kotlin_version: String by extra { "1.1.11" }'
                .addBuildscriptPropertyDefinition("var $GSK_KOTLIN_VERSION_PROPERTY_NAME: String by extra\n    $GSK_KOTLIN_VERSION_PROPERTY_NAME = \"$kotlinVersion\"")
                .addBuildscriptDependencyNotation(getKotlinGradlePluginClassPathSnippet())
        }

        buildScriptData.addRepositoriesDefinition("mavenCentral()")

        val isNewProject = module.project.getUserData(ExternalSystemDataKeys.NEWLY_CREATED_PROJECT) == true
        if (isNewProject) {
            ProjectCodeStyleImporter.apply(module.project, KotlinStyleGuideCodeStyle.INSTANCE)
            GradlePropertiesFileFacade.forProject(module.project).addCodeStyleProperty(KotlinStyleGuideCodeStyle.CODE_STYLE_SETTING)
        }

        KotlinFUSLogger.log(FUSEventGroups.NPWizards, this.javaClass.simpleName)
    }

    private fun RepositoryDescription.toKotlinRepositorySnippet() = "maven { setUrl(\"$url\") }"

    protected abstract fun getOldSyntaxPluginDefinition(): String
    protected abstract fun getPluginDefinition(): String

    protected fun composeDependency(buildScriptData: BuildScriptDataBuilder, artifactId: String): String {
        return if (buildScriptData.gradleVersion >= MIN_GRADLE_VERSION_FOR_NEW_PLUGIN_SYNTAX)
            "implementation(${getKotlinModuleDependencySnippet(artifactId, null)})"
        else
            "implementation(${getKotlinModuleDependencySnippet(artifactId, "\$$GSK_KOTLIN_VERSION_PROPERTY_NAME")})"
    }
}

class KotlinDslGradleKotlinJavaFrameworkSupportProvider :
    KotlinDslGradleKotlinFrameworkSupportProvider("KOTLIN", "Kotlin/JVM", KotlinIcons.SMALL_LOGO) {

    override fun getOldSyntaxPluginDefinition() = "plugin(\"${KotlinGradleModuleConfigurator.KOTLIN}\")"
    override fun getPluginDefinition() = "kotlin(\"jvm\")"

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

        val artifactId = getStdlibArtifactId(rootModel.sdk, bundledRuntimeVersion())
        buildScriptData.addDependencyNotation(composeDependency(buildScriptData, artifactId))
    }
}

abstract class AbstractKotlinDslGradleKotlinJSFrameworkSupportProvider(
    frameworkTypeId: String,
    displayName: String
) : KotlinDslGradleKotlinFrameworkSupportProvider(frameworkTypeId, displayName, KotlinIcons.JS) {
    abstract val jsSubTargetName: String

    override fun addSupport(
        projectId: ProjectId,
        module: Module,
        rootModel: ModifiableRootModel,
        modifiableModelsProvider: ModifiableModelsProvider,
        buildScriptData: BuildScriptDataBuilder
    ) {
        super.addSupport(projectId, module, rootModel, modifiableModelsProvider, buildScriptData)

        buildScriptData.addOther("kotlin.target.$jsSubTargetName { }")
        val artifactId = MAVEN_JS_STDLIB_ID.removePrefix("kotlin-")
        buildScriptData.addDependencyNotation(composeDependency(buildScriptData, artifactId))
    }

    override fun getOldSyntaxPluginDefinition(): String = "plugin(\"${KotlinJsGradleModuleConfigurator.KOTLIN_JS}\")"
    override fun getPluginDefinition(): String = "id(\"org.jetbrains.kotlin.js\")"

}

class KotlinDslGradleKotlinJSBrowserFrameworkSupportProvider :
    AbstractKotlinDslGradleKotlinJSFrameworkSupportProvider("KOTLIN_JS_BROWSER", "Kotlin/JS for browser") {
    override val jsSubTargetName: String
        get() = "browser"
}

class KotlinDslGradleKotlinJSNodeFrameworkSupportProvider :
    AbstractKotlinDslGradleKotlinJSFrameworkSupportProvider("KOTLIN_JS_NODE", "Kotlin/JS for Node.js") {
    override val jsSubTargetName: String
        get() = "nodejs"
}

class KotlinDslGradleKotlinMPPFrameworkSupportProvider :
    KotlinDslGradleKotlinFrameworkSupportProvider("KOTLIN_MPP", "Kotlin/Multiplatform", KotlinIcons.MPP) {

    override fun getOldSyntaxPluginDefinition() = "plugin(\"org.jetbrains.kotlin.multiplatform\")"
    override fun getPluginDefinition() = "kotlin(\"multiplatform\")"

    override fun addSupport(
        projectId: ProjectId,
        module: Module,
        rootModel: ModifiableRootModel,
        modifiableModelsProvider: ModifiableModelsProvider,
        buildScriptData: BuildScriptDataBuilder
    ) {
        super.addSupport(projectId, module, rootModel, modifiableModelsProvider, buildScriptData)

        buildScriptData.addOther(
            """kotlin {
    /* Targets configuration omitted. 
    *  To find out how to configure the targets, please follow the link:
    *  https://kotlinlang.org/docs/reference/building-mpp-with-gradle.html#setting-up-targets */

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
    }
}"""
        )
    }
}