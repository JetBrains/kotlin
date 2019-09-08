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
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys
import com.intellij.openapi.module.Module
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ModifiableModelsProvider
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil.createDirectoryIfMissing
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.idea.formatter.KotlinStyleGuideCodeStyle
import org.jetbrains.kotlin.idea.formatter.ProjectCodeStyleImporter
import org.jetbrains.kotlin.idea.statistics.FUSEventGroups
import org.jetbrains.kotlin.idea.statistics.KotlinFUSLogger
import org.jetbrains.kotlin.idea.util.isSnapshot
import org.jetbrains.kotlin.idea.versions.*
import org.jetbrains.plugins.gradle.frameworkSupport.BuildScriptDataBuilder
import org.jetbrains.plugins.gradle.frameworkSupport.GradleFrameworkSupportProvider
import java.io.File
import java.io.Writer
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JTextPane

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

            override fun createComponent(): JComponent {
                val jTextPane = JTextPane()
                jTextPane.text = getDescription()
                return jTextPane
            }
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

    open fun addSupport(
        buildScriptData: BuildScriptDataBuilder,
        module: Module,
        sdk: Sdk?,
        specifyPluginVersionIfNeeded: Boolean,
        explicitPluginVersion: String? = null
    ) {
        var kotlinVersion = explicitPluginVersion ?: bundledRuntimeVersion()
        val additionalRepository = getRepositoryForVersion(kotlinVersion)
        if (isSnapshot(kotlinVersion)) {
            kotlinVersion = LAST_SNAPSHOT_VERSION
        }

        val gradleVersion = buildScriptData.gradleVersion
        val useNewSyntax = gradleVersion >= MIN_GRADLE_VERSION_FOR_NEW_PLUGIN_SYNTAX
        if (useNewSyntax) {
            if (additionalRepository != null) {
                val oneLineRepository = additionalRepository.toGroovyRepositorySnippet().replace('\n', ' ')
                updateSettingsScript(module) {
                    with(it) {
                        addPluginRepository(additionalRepository)
                        addMavenCentralPluginRepository()
                        addPluginRepository(DEFAULT_GRADLE_PLUGIN_REPOSITORY)
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
            buildScriptData.addDependencyNotation(
                KotlinWithGradleConfigurator.getGroovyDependencySnippet(dependency, "implementation", !useNewSyntax, gradleVersion)
            )
        }
        for (dependency in getTestDependencies()) {
            buildScriptData.addDependencyNotation(
                if (":" in dependency)
                    "${gradleVersion.scope("testImplementation")} \"$dependency\""
                else
                    KotlinWithGradleConfigurator.getGroovyDependencySnippet(dependency, "testImplementation", !useNewSyntax, gradleVersion)
            )
        }

        if (useNewSyntax) {
            updateSettingsScript(module) { updateSettingsScript(it, specifyPluginVersionIfNeeded) }
        } else {
            buildScriptData.addBuildscriptDependencyNotation(KotlinWithGradleConfigurator.CLASSPATH)
        }

        val isNewProject = module.project.getUserData(ExternalSystemDataKeys.NEWLY_CREATED_PROJECT) == true
        if (isNewProject) {
            ProjectCodeStyleImporter.apply(module.project, KotlinStyleGuideCodeStyle.INSTANCE)
            GradlePropertiesFileFacade.forProject(module.project).addCodeStyleProperty(KotlinStyleGuideCodeStyle.CODE_STYLE_SETTING)
        }
        KotlinFUSLogger.log(FUSEventGroups.NPWizards, this.javaClass.simpleName)
    }

    protected open fun updateSettingsScript(settingsBuilder: SettingsScriptBuilder, specifyPluginVersionIfNeeded: Boolean) {}

    protected abstract fun getDependencies(sdk: Sdk?): List<String>
    protected open fun getTestDependencies(): List<String> = listOf()

    protected abstract fun getPluginId(): String
    protected abstract fun getPluginExpression(): String

    protected abstract fun getDescription(): String
}

open class GradleKotlinJavaFrameworkSupportProvider(
    frameworkTypeId: String = "KOTLIN",
    displayName: String = "Kotlin/JVM"
) : GradleKotlinFrameworkSupportProvider(frameworkTypeId, displayName, KotlinIcons.SMALL_LOGO) {

    override fun getPluginId() = KotlinGradleModuleConfigurator.KOTLIN
    override fun getPluginExpression() = "id 'org.jetbrains.kotlin.jvm'"

    override fun getDependencies(sdk: Sdk?) = listOf(getStdlibArtifactId(sdk, bundledRuntimeVersion()))

    override fun addSupport(
        buildScriptData: BuildScriptDataBuilder,
        module: Module,
        sdk: Sdk?,
        specifyPluginVersionIfNeeded: Boolean,
        explicitPluginVersion: String?
    ) {
        super.addSupport(buildScriptData, module, sdk, specifyPluginVersionIfNeeded, explicitPluginVersion)
        val jvmTarget = getDefaultJvmTarget(sdk, bundledRuntimeVersion())
        if (jvmTarget != null) {
            val description = jvmTarget.description
            buildScriptData.addOther("compileKotlin {\n    kotlinOptions.jvmTarget = \"$description\"\n}\n\n")
            buildScriptData.addOther("compileTestKotlin {\n    kotlinOptions.jvmTarget = \"$description\"\n}\n")
        }
    }

    override fun getDescription() = "A single-platform Kotlin library or application targeting the JVM"
}

abstract class GradleKotlinJSFrameworkSupportProvider(
    frameworkTypeId: String,
    displayName: String
) : GradleKotlinFrameworkSupportProvider(frameworkTypeId, displayName, KotlinIcons.JS) {
    abstract val jsSubTargetName: String

    override fun addSupport(
        buildScriptData: BuildScriptDataBuilder,
        module: Module,
        sdk: Sdk?,
        specifyPluginVersionIfNeeded: Boolean,
        explicitPluginVersion: String?
    ) {
        super.addSupport(buildScriptData, module, sdk, specifyPluginVersionIfNeeded, explicitPluginVersion)

        buildScriptData.addOther("kotlin.target.$jsSubTargetName { }")
    }

    override fun getPluginId() = KotlinJsGradleModuleConfigurator.KOTLIN_JS
    override fun getPluginExpression() = "id 'org.jetbrains.kotlin.js'"
    override fun getDependencies(sdk: Sdk?) = listOf(MAVEN_JS_STDLIB_ID)
    override fun getTestDependencies() = listOf(MAVEN_JS_TEST_ID)
    override fun getDescription() = "A single-platform Kotlin library or application targeting JavaScript"
}

open class GradleKotlinJSBrowserFrameworkSupportProvider(
    frameworkTypeId: String = "KOTLIN_JS_BROWSER",
    displayName: String = "Kotlin/JS for browser"
) : GradleKotlinJSFrameworkSupportProvider(frameworkTypeId, displayName) {
    override val jsSubTargetName: String
        get() = "browser"

    override fun getDescription() = "A single-platform Kotlin library or application targeting JavaScript for browser"

    override fun addSupport(
        buildScriptData: BuildScriptDataBuilder,
        module: Module,
        sdk: Sdk?,
        specifyPluginVersionIfNeeded: Boolean,
        explicitPluginVersion: String?
    ) {
        super.addSupport(buildScriptData, module, sdk, specifyPluginVersionIfNeeded, explicitPluginVersion)

        getNewFileWriter(module, "src/main/resources", "index.html")?.use {
            it.write("""
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <title>${module.name}</title>
                    <script src="${module.name}.js"></script>
                </head>
                <body>
                
                </body>
                </html>
            """.trimIndent().trim())
        }


        getNewFileWriter(module, "src/main/kotlin", "main.kt")?.use {
            it.write("""
                import kotlin.browser.document
                
                fun main() {
                    document.write("Hello, world!")
                }
            """.trimIndent().trim())
        }
    }

    /**
     * create parent directories and file
     * @return null if file already exists
     */
    private fun getNewFileWriter(module: Module, relativeDir: String, fileName: String): Writer? {
        val contentEntryPath = module.gradleModuleBuilder?.contentEntryPath ?: return null
        if (contentEntryPath.isEmpty()) return null
        val contentRootDir = File(contentEntryPath)
        val modelContentRootDir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(contentRootDir) ?: return null

        val dir = createDirectoryIfMissing(modelContentRootDir, relativeDir) ?: return null
        if (dir.findChild(fileName) != null) return null
        val file = dir.createChildData(null, fileName)
        return file.getOutputStream(null).writer()
    }
}

open class GradleKotlinJSNodeFrameworkSupportProvider(
    frameworkTypeId: String = "KOTLIN_JS_NODE",
    displayName: String = "Kotlin/JS for Node.js"
) : GradleKotlinJSFrameworkSupportProvider(frameworkTypeId, displayName) {
    override val jsSubTargetName: String
        get() = "nodejs"

    override fun getDescription() = "A single-platform Kotlin library or application targeting JavaScript for Node.js"
}

open class GradleKotlinMPPFrameworkSupportProvider : GradleKotlinFrameworkSupportProvider(
    "KOTLIN_MPP", "Kotlin/Multiplatform", KotlinIcons.MPP
) {
    override fun getPluginId() = "org.jetbrains.kotlin.multiplatform"
    override fun getPluginExpression() = "id 'org.jetbrains.kotlin.multiplatform'"

    override fun getDependencies(sdk: Sdk?): List<String> = listOf()
    override fun getTestDependencies(): List<String> = listOf()

    override fun getDescription() =
        "Multi-targeted (JVM, JS, iOS, etc.) project with shared code in common modules. " +
                "The targets can be configured in the project's build script."
}

open class GradleKotlinMPPSourceSetsFrameworkSupportProvider : GradleKotlinMPPFrameworkSupportProvider() {

    override fun addSupport(
        buildScriptData: BuildScriptDataBuilder,
        module: Module,
        sdk: Sdk?,
        specifyPluginVersionIfNeeded: Boolean,
        explicitPluginVersion: String?
    ) {
        super.addSupport(buildScriptData, module, sdk, specifyPluginVersionIfNeeded, explicitPluginVersion)

        buildScriptData.addOther(
            """kotlin {
    /* Targets configuration omitted. 
    *  To find out how to configure the targets, please follow the link:
    *  https://kotlinlang.org/docs/reference/building-mpp-with-gradle.html#setting-up-targets */

    sourceSets {
        commonMain {
            dependencies {
                implementation kotlin('stdlib-common')
            }
        }
        commonTest {
            dependencies {
                implementation kotlin('test-common')
                implementation kotlin('test-annotations-common')
            }
        }
    }
}"""
        )
    }
}

