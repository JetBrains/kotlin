/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle

import com.intellij.execution.configurations.CommandLineTokenizer
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import com.intellij.util.EnvironmentUtil
import com.intellij.util.PathUtil
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.idea.KotlinIdeaGradleBundle
import org.jetbrains.kotlin.idea.core.script.*
import org.jetbrains.kotlin.idea.scripting.gradle.importing.KotlinDslSyncListener
import org.jetbrains.kotlin.idea.scripting.gradle.roots.GradleBuildRootsManager
import org.jetbrains.kotlin.idea.scripting.gradle.roots.Imported
import org.jetbrains.kotlin.idea.scripting.gradle.roots.WithoutScriptModels
import org.jetbrains.kotlin.scripting.definitions.KotlinScriptDefinitionAdapterFromNewAPIBase
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.definitions.getEnvironment
import org.jetbrains.kotlin.scripting.resolve.KotlinScriptDefinitionFromAnnotatedTemplate
import org.jetbrains.plugins.gradle.config.GradleSettingsListenerAdapter
import org.jetbrains.plugins.gradle.service.GradleInstallationManager
import org.jetbrains.plugins.gradle.settings.DistributionType
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings
import org.jetbrains.plugins.gradle.settings.GradleSettingsListener
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.script.dependencies.Environment
import kotlin.script.dependencies.ScriptContents
import kotlin.script.experimental.api.*
import kotlin.script.experimental.dependencies.DependenciesResolver
import kotlin.script.experimental.dependencies.DependenciesResolver.ResolveResult
import kotlin.script.experimental.dependencies.ScriptReport
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.templates.standard.ScriptTemplateWithArgs

class GradleScriptDefinitionsContributor(private val project: Project) : ScriptDefinitionSourceAsContributor {
    companion object {
        fun getDefinitions(project: Project, workingDir: String, gradleHome: String?, javaHome: String?): List<ScriptDefinition>? {
            val contributor = ScriptDefinitionContributor.EP_NAME.getExtensions(project)
                .filterIsInstance<GradleScriptDefinitionsContributor>()
                .singleOrNull()

            if (contributor == null) {
                scriptingInfoLog(
                    "cannot find gradle script definitions contributor in ScriptDefinitionContributor.EP_NAME list: " +
                            "workingDir=$workingDir gradleHome=$gradleHome"
                )
                return null
            }
            if (gradleHome == null) {
                scriptingInfoLog(KotlinIdeaGradleBundle.message("error.text.unable.to.get.gradle.home.directory") + ": workingDir=$workingDir gradleHome=$gradleHome")
                return null
            }

            val root = LightGradleBuildRoot(workingDir, gradleHome, javaHome)
            with(contributor) {
                if (root.isError()) return null
            }

            val definitions = contributor.definitionsByRoots[root]
            if (definitions == null) {
                scriptingInfoLog(
                    "script definitions aren't loaded yet. " +
                            "They should be loaded by invoking GradleScriptDefinitionsContributor.reloadIfNeeded from KotlinDslSyncListener: " +
                            "workingDir=$workingDir gradleHome=$gradleHome"
                )
                return null
            }
            return definitions
        }

        fun getDefinitionsTemplateClasspath(gradleHome: String?): List<String> {
            return try {
                getFullDefinitionsClasspath(gradleHome).first.map {
                    PathUtil.toSystemIndependentName(it.path)
                }
            } catch (e: Throwable) {
                scriptingInfoLog("cannot get gradle classpath for Gradle Kotlin DSL scripts: ${e.message}")

                emptyList()
            }
        }

        private val kotlinDslDependencySelector = Regex("^gradle-(?:kotlin-dsl|core).*\\.jar\$")

        private fun getFullDefinitionsClasspath(gradleHome: String?): Pair<List<File>, List<File>> {
            if (gradleHome == null) {
                error(KotlinIdeaGradleBundle.message("error.text.unable.to.get.gradle.home.directory"))
            }

            val gradleLibDir = File(gradleHome, "lib")
                .let {
                    it.takeIf { it.exists() && it.isDirectory }
                        ?: error(KotlinIdeaGradleBundle.message("error.text.invalid.gradle.libraries.directory", it))
                }

            val templateClasspath = gradleLibDir
                /* an inference problem without explicit 'it', TODO: remove when fixed */
                .listFiles { it -> kotlinDslDependencySelector.matches(it.name) }
                ?.takeIf { it.isNotEmpty() }
                ?.asList()
                ?: error(KotlinIdeaGradleBundle.message("error.text.missing.jars.in.gradle.directory"))

            scriptingDebugLog { "gradle script templates classpath $templateClasspath" }

            val additionalClassPath = kotlinStdlibAndCompiler(gradleLibDir)

            scriptingDebugLog { "gradle script templates additional classpath $templateClasspath" }

            return templateClasspath to additionalClassPath
        }

        // TODO: check this against kotlin-dsl branch that uses daemon
        private fun kotlinStdlibAndCompiler(gradleLibDir: File): List<File> {
            // additionally need compiler jar to load gradle resolver
            return gradleLibDir.listFiles { file ->
                file?.name?.startsWith("kotlin-compiler-embeddable") == true || file?.name?.startsWith("kotlin-stdlib") == true
            }?.firstOrNull()?.let(::listOf).orEmpty()
        }

        private val kotlinStdLibSelector = Regex("^(kotlin-compiler-embeddable|kotlin-stdlib)-(\\d+\\.\\d+).*\\.jar\$")

        fun findStdLibLanguageVersion(classpath: List<File>): LanguageVersion? {
            return classpath.map { it.parentFile }.toSet().map {
                it.listFiles { file ->
                    kotlinStdLibSelector.find(file.name) != null
                }.firstOrNull()?.let { file ->
                    val matchResult = kotlinStdLibSelector.find(file.name) ?: return@let null
                    LanguageVersion.fromVersionString(matchResult.groupValues[2])
                }
            }.firstOrNull()
        }
    }

    override val id: String = "Gradle Kotlin DSL"

    private data class LightGradleBuildRoot(val workingDir: String, val gradleHome: String?, val javaHome: String?)

    private val definitionsByRoots = ConcurrentHashMap<LightGradleBuildRoot, List<ScriptDefinition>>()

    private fun LightGradleBuildRoot.markAsError() {
        definitionsByRoots[this] = listOf(ErrorGradleScriptDefinition(project))
    }

    private fun LightGradleBuildRoot.isError(): Boolean {
        return definitionsByRoots[this]?.any { it is ErrorGradleScriptDefinition } ?: false
    }

    private fun forceReload() {
        for (key in definitionsByRoots.keys()) {
            key.markAsError()
        }
        ScriptDefinitionsManager.getInstance(project).reloadDefinitionsBy(this)
    }

    // TODO: remove old roots
    fun reloadIfNeeded(workingDir: String, gradleHome: String?, javaHome: String?) {
        val root = LightGradleBuildRoot(workingDir, gradleHome, javaHome)
        val value = definitionsByRoots[root]
        if (value != null) {
            if (root.isError()) {
                ScriptDefinitionsManager.getInstance(project).reloadDefinitionsBy(this)
            }
        } else {
            val rootWithChangedGradleHome = definitionsByRoots.filter { it.key.workingDir == workingDir }
            if (rootWithChangedGradleHome.isNotEmpty()) {
                rootWithChangedGradleHome.forEach {
                    definitionsByRoots.remove(it.key)
                }
            }
            root.markAsError()
            ScriptDefinitionsManager.getInstance(project).reloadDefinitionsBy(this)
        }
    }

    private fun loadGradleDefinitions(root: LightGradleBuildRoot): List<ScriptDefinition> {
        try {
            val (templateClasspath, additionalClassPath) = getFullDefinitionsClasspath(root.gradleHome)

            val kotlinDslTemplates = ArrayList<ScriptDefinition>()

            val projectPath = root.workingDir
            loadGradleTemplates(
                projectPath,
                templateClass = "org.gradle.kotlin.dsl.KotlinInitScript",
                gradleHome = root.gradleHome,
                javaHome = root.javaHome,
                templateClasspath = templateClasspath,
                additionalClassPath = additionalClassPath
            ).let { kotlinDslTemplates.addAll(it) }

            loadGradleTemplates(
                projectPath,
                templateClass = "org.gradle.kotlin.dsl.KotlinSettingsScript",
                gradleHome = root.gradleHome,
                javaHome = root.javaHome,
                templateClasspath = templateClasspath,
                additionalClassPath = additionalClassPath
            ).let { kotlinDslTemplates.addAll(it) }

            // KotlinBuildScript should be last because it has wide scriptFilePattern
            loadGradleTemplates(
                projectPath,
                templateClass = "org.gradle.kotlin.dsl.KotlinBuildScript",
                gradleHome = root.gradleHome,
                javaHome = root.javaHome,
                templateClasspath = templateClasspath,
                additionalClassPath = additionalClassPath
            ).let { kotlinDslTemplates.addAll(it) }


            if (kotlinDslTemplates.isNotEmpty()) {
                return kotlinDslTemplates.distinct()
            }
        } catch (t: Throwable) {
            // TODO: review exception handling

            if (t is IllegalStateException) {
                scriptingInfoLog("IllegalStateException loading gradle script templates: ${t.message}")
            } else {
                scriptingDebugLog { "error loading gradle script templates ${t.message}" }
            }

            return listOf(ErrorGradleScriptDefinition(project, t.message))
        }

        return listOf(ErrorGradleScriptDefinition(project))

    }

    private fun loadGradleTemplates(
        projectPath: String,
        templateClass: String,
        gradleHome: String?,
        javaHome: String?,
        templateClasspath: List<File>,
        additionalClassPath: List<File>
    ): List<ScriptDefinition> {
        val gradleExeSettings = ExternalSystemApiUtil.getExecutionSettings<GradleExecutionSettings>(
            project,
            projectPath,
            GradleConstants.SYSTEM_ID
        )
        val defaultCompilerOptions = findStdLibLanguageVersion(templateClasspath)?.let {
            listOf("-language-version", it.versionString)
        } ?: emptyList<String>()
        val hostConfiguration = createHostConfiguration(projectPath, gradleHome, javaHome, gradleExeSettings)
        return loadDefinitionsFromTemplates(
            listOf(templateClass),
            templateClasspath,
            hostConfiguration,
            additionalClassPath,
            defaultCompilerOptions
        ).map {
            it.asLegacyOrNull<KotlinScriptDefinitionFromAnnotatedTemplate>()?.let { legacyDef ->
                // Expand scope for old gradle script definition
                val version = GradleInstallationManager.getGradleVersion(gradleHome) ?: GradleVersion.current().version
                GradleKotlinScriptDefinitionWrapper(
                    it.hostConfiguration,
                    legacyDef,
                    version,
                    defaultCompilerOptions
                )
            } ?: it
        }
    }

    private fun createHostConfiguration(
        projectPath: String,
        gradleHome: String?,
        javaHome: String?,
        gradleExeSettings: GradleExecutionSettings
    ): ScriptingHostConfiguration {
        val gradleJvmOptions = gradleExeSettings.daemonVmOptions?.let { vmOptions ->
            CommandLineTokenizer(vmOptions).toList()
                .mapNotNull { it?.let { it as? String } }
                .filterNot(String::isBlank)
                .distinct()
        } ?: emptyList()

        val environment = mapOf(
            "gradleHome" to gradleHome?.let(::File),
            "gradleJavaHome" to javaHome,

            "projectRoot" to projectPath.let(::File),

            "gradleOptions" to emptyList<String>(), // There is no option in UI to set project wide gradleOptions
            "gradleJvmOptions" to gradleJvmOptions,
            "gradleEnvironmentVariables" to if (gradleExeSettings.isPassParentEnvs) EnvironmentUtil.getEnvironmentMap() else emptyMap()
        )
        return ScriptingHostConfiguration(defaultJvmScriptingHostConfiguration) {
            getEnvironment { environment }
        }
    }

    init {
        subscribeToGradleSettingChanges()
    }

    private fun subscribeToGradleSettingChanges() {
        val listener = object : GradleSettingsListenerAdapter() {
            override fun onGradleVmOptionsChange(oldOptions: String?, newOptions: String?) {
                forceReload()
            }

            override fun onGradleHomeChange(oldPath: String?, newPath: String?, linkedProjectPath: String) {
                forceReload()
            }

            override fun onServiceDirectoryPathChange(oldPath: String?, newPath: String?) {
                forceReload()
            }

            override fun onGradleDistributionTypeChange(currentValue: DistributionType?, linkedProjectPath: String) {
                forceReload()
            }
        }
        project.messageBus.connect().subscribe(GradleSettingsListener.TOPIC, listener)
    }

    // NOTE: control flow here depends on suppressing exceptions from loadGradleTemplates calls
    // TODO: possibly combine exceptions from every loadGradleTemplates call, be mindful of KT-19276
    override val definitions: Sequence<ScriptDefinition>
        get() {
            definitionsByRoots.keys().iterator().forEachRemaining { root ->
                // reload definitions marked as error
                if (root.isError()) {
                    definitionsByRoots[root] = loadGradleDefinitions(root)
                }
            }
            if (definitionsByRoots.isEmpty()) {
                // can be empty in case when import wasn't done from IDE start up,
                // otherwise KotlinDslSyncListener should run reloadIfNeeded for valid roots
                GradleBuildRootsManager.getInstance(project).getAllRoots().forEach {
                    val workingDir = it.pathPrefix
                    val (gradleHome, javaHome) = when (it) {
                        is Imported -> {
                            it.data.gradleHome to it.data.javaHome
                        }
                        is WithoutScriptModels -> {
                            val settings = ExternalSystemApiUtil.getExecutionSettings<GradleExecutionSettings>(
                                project,
                                workingDir,
                                GradleConstants.SYSTEM_ID
                            )
                            settings.gradleHome to settings.javaHome
                        }
                    }
                    val root = LightGradleBuildRoot(workingDir, gradleHome, javaHome)
                    definitionsByRoots[root] = loadGradleDefinitions(root)
                }
            }
            if (definitionsByRoots.isNotEmpty()) {
                return definitionsByRoots.flatMap { it.value }.asSequence()
            }
            return sequenceOf(ErrorGradleScriptDefinition(project))
        }


}

// TODO: refactor - minimize
private class ErrorGradleScriptDefinition(project: Project, message: String? = null) :
    ScriptDefinition.FromLegacy(
        ScriptingHostConfiguration(defaultJvmScriptingHostConfiguration),
        LegacyDefinition(project, message)
    ) {

    private class LegacyDefinition(project: Project, message: String?) : KotlinScriptDefinitionAdapterFromNewAPIBase() {
        companion object {
            private const val KOTLIN_DSL_SCRIPT_EXTENSION = "gradle.kts"
        }

        override val name: String get() = KotlinIdeaGradleBundle.message("text.default.kotlin.gradle.script")
        override val fileExtension: String = KOTLIN_DSL_SCRIPT_EXTENSION

        override val scriptCompilationConfiguration: ScriptCompilationConfiguration = ScriptCompilationConfiguration.Default
        override val hostConfiguration: ScriptingHostConfiguration = ScriptingHostConfiguration(defaultJvmScriptingHostConfiguration)
        override val baseClass: KClass<*> = ScriptTemplateWithArgs::class

        override val dependencyResolver: DependenciesResolver =
            ErrorScriptDependenciesResolver(project, message)

        override fun toString(): String = "ErrorGradleScriptDefinition"
    }

    override fun equals(other: Any?): Boolean = other is ErrorGradleScriptDefinition
    override fun hashCode(): Int = name.hashCode()
}

private class ErrorScriptDependenciesResolver(
    private val project: Project,
    private val message: String? = null
) : DependenciesResolver {
    override fun resolve(scriptContents: ScriptContents, environment: Environment): ResolveResult {
        val importTasks = KotlinDslSyncListener.instance.tasks
        val importInProgress = synchronized(importTasks) { importTasks.values.any { it.project == project } }
        val failureMessage = if (importInProgress) {
            KotlinIdeaGradleBundle.message("error.text.highlighting.is.impossible.during.gradle.import")
        } else {
            message ?: KotlinIdeaGradleBundle.message(
                "error.text.failed.to.load.script.definitions.by",
                GradleScriptDefinitionsContributor::class.java.name
            )
        }
        return ResolveResult.Failure(ScriptReport(failureMessage, ScriptReport.Severity.FATAL))
    }
}

