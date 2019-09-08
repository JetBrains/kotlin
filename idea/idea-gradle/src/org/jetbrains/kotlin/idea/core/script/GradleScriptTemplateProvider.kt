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

package org.jetbrains.kotlin.idea.core.script

import com.intellij.execution.configurations.CommandLineTokenizer
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListenerAdapter
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import com.intellij.util.EnvironmentUtil
import org.jetbrains.kotlin.idea.framework.GRADLE_SYSTEM_ID
import org.jetbrains.kotlin.lexer.KotlinLexer
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.scripting.definitions.KotlinScriptDefinitionAdapterFromNewAPIBase
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.definitions.getEnvironment
import org.jetbrains.kotlin.scripting.resolve.KotlinScriptDefinitionFromAnnotatedTemplate
import org.jetbrains.plugins.gradle.config.GradleSettingsListenerAdapter
import org.jetbrains.plugins.gradle.settings.DistributionType
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.settings.GradleSettingsListener
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.reflect.KClass
import kotlin.script.dependencies.Environment
import kotlin.script.dependencies.ScriptContents
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.dependencies.DependenciesResolver
import kotlin.script.experimental.dependencies.DependenciesResolver.ResolveResult
import kotlin.script.experimental.dependencies.ScriptReport
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.location.ScriptExpectedLocation
import kotlin.script.templates.standard.ScriptTemplateWithArgs

class GradleScriptDefinitionsContributor(private val project: Project) : ScriptDefinitionSourceAsContributor {

    override val id: String = "Gradle Kotlin DSL"
    private val failedToLoad = AtomicBoolean(false)

    init {
        subscribeToGradleSettingChanges()
    }

    private fun subscribeToGradleSettingChanges() {
        val listener = object : GradleSettingsListenerAdapter() {
            override fun onGradleVmOptionsChange(oldOptions: String?, newOptions: String?) {
                reload()
            }

            override fun onGradleHomeChange(oldPath: String?, newPath: String?, linkedProjectPath: String) {
                reload()
            }

            override fun onServiceDirectoryPathChange(oldPath: String?, newPath: String?) {
                reload()
            }

            override fun onGradleDistributionTypeChange(currentValue: DistributionType?, linkedProjectPath: String) {
                reload()
            }
        }
        project.messageBus.connect(project).subscribe(GradleSettingsListener.TOPIC, listener)
    }

    // NOTE: control flow here depends on suppressing exceptions from loadGradleTemplates calls
    // TODO: possibly combine exceptions from every loadGradleTemplates call, be mindful of KT-19276
    override val definitions: Sequence<ScriptDefinition>
        get() {
            val kotlinDslDependencySelector = Regex("^gradle-(?:kotlin-dsl|core).*\\.jar\$")
            val kotlinDslAdditionalResolverCp = ::kotlinStdlibAndCompiler

            failedToLoad.set(false)

            val kotlinDslTemplates = LinkedHashSet<ScriptDefinition>()

            loadGradleTemplates(
                templateClass = "org.gradle.kotlin.dsl.KotlinInitScript",
                dependencySelector = kotlinDslDependencySelector,
                additionalResolverClasspath = kotlinDslAdditionalResolverCp

            ).let { kotlinDslTemplates.addAll(it) }

            loadGradleTemplates(
                templateClass = "org.gradle.kotlin.dsl.KotlinSettingsScript",
                dependencySelector = kotlinDslDependencySelector,
                additionalResolverClasspath = kotlinDslAdditionalResolverCp

            ).let { kotlinDslTemplates.addAll(it) }

            // KotlinBuildScript should be last because it has wide scriptFilePattern
            loadGradleTemplates(
                templateClass = "org.gradle.kotlin.dsl.KotlinBuildScript",
                dependencySelector = kotlinDslDependencySelector,
                additionalResolverClasspath = kotlinDslAdditionalResolverCp
            ).let { kotlinDslTemplates.addAll(it) }


            if (kotlinDslTemplates.isNotEmpty()) {
                return kotlinDslTemplates.asSequence()
            }

            val default = tryToLoadOldBuildScriptDefinition()
            if (default.isNotEmpty()) {
                return default.asSequence()
            }

            return sequenceOf(ErrorGradleScriptDefinition())
        }

    private fun tryToLoadOldBuildScriptDefinition(): List<ScriptDefinition> {
        failedToLoad.set(false)

        return loadGradleTemplates(
            templateClass = "org.gradle.script.lang.kotlin.KotlinBuildScript",
            dependencySelector = Regex("^gradle-(?:script-kotlin|core).*\\.jar\$"),
            additionalResolverClasspath = { emptyList() }
        )
    }

    // TODO: check this against kotlin-dsl branch that uses daemon
    private fun kotlinStdlibAndCompiler(gradleLibDir: File): List<File> {
        // additionally need compiler jar to load gradle resolver
        return gradleLibDir.listFiles { file -> file.name.startsWith("kotlin-compiler-embeddable") || file.name.startsWith("kotlin-stdlib") }
            .firstOrNull()?.let(::listOf).orEmpty()
    }

    private fun loadGradleTemplates(
        templateClass: String, dependencySelector: Regex,
        additionalResolverClasspath: (gradleLibDir: File) -> List<File>
    ): List<ScriptDefinition> = try {
        doLoadGradleTemplates(templateClass, dependencySelector, additionalResolverClasspath)
    } catch (t: Throwable) {
        // TODO: review exception handling
        failedToLoad.set(true)
        if (t is IllegalStateException) {
            Logger.getInstance(GradleScriptDefinitionsContributor::class.java)
                .info("[kts] error loading gradle script templates: ${t.message}")
        }
        listOf(ErrorGradleScriptDefinition(t.message))
    }


    private fun doLoadGradleTemplates(
        templateClass: String, dependencySelector: Regex,
        additionalResolverClasspath: (gradleLibDir: File) -> List<File>
    ): List<ScriptDefinition> {
        fun createHostConfiguration(
            gradleExeSettings: GradleExecutionSettings,
            projectSettings: GradleProjectSettings
        ): ScriptingHostConfiguration {
            val gradleJvmOptions = gradleExeSettings.daemonVmOptions?.let { vmOptions ->
                CommandLineTokenizer(vmOptions).toList()
                    .mapNotNull { it?.let { it as? String } }
                    .filterNot(String::isBlank)
                    .distinct()
            } ?: emptyList()


            val environment = mapOf(
                "gradleHome" to gradleExeSettings.gradleHome?.let(::File),
                "gradleJavaHome" to gradleExeSettings.javaHome,

                "projectRoot" to projectSettings.externalProjectPath.let(::File),

                "gradleOptions" to emptyList<String>(), // There is no option in UI to set project wide gradleOptions
                "gradleJvmOptions" to gradleJvmOptions,
                "gradleEnvironmentVariables" to if (gradleExeSettings.isPassParentEnvs) EnvironmentUtil.getEnvironmentMap() else emptyMap(),

                "getScriptSectionTokens" to ::topLevelSectionCodeTextTokens
            )
            return ScriptingHostConfiguration(defaultJvmScriptingHostConfiguration) {
                getEnvironment { environment }
            }
        }

        val gradleSettings = ExternalSystemApiUtil.getSettings(project, GradleConstants.SYSTEM_ID)
        if (gradleSettings.getLinkedProjectsSettings().isEmpty()) error("Project '${project.name}' isn't linked with Gradle")

        val projectSettings = gradleSettings.getLinkedProjectsSettings().filterIsInstance<GradleProjectSettings>().firstOrNull()
            ?: error("Project '${project.name}' isn't linked with Gradle")

        val gradleExeSettings = ExternalSystemApiUtil.getExecutionSettings<GradleExecutionSettings>(
            project,
            projectSettings.externalProjectPath,
            GradleConstants.SYSTEM_ID
        )

        val gradleHome = gradleExeSettings.gradleHome ?: error("Unable to get Gradle home directory")

        val gradleLibDir = File(gradleHome, "lib").let {
            it.takeIf { it.exists() && it.isDirectory } ?: error("Invalid Gradle libraries directory $it")
        }
        val templateClasspath = gradleLibDir.listFiles { it ->
            /* an inference problem without explicit 'it', TODO: remove when fixed */
            dependencySelector.matches(it.name)
        }.takeIf { it.isNotEmpty() }?.asList() ?: error("Missing jars in gradle directory")

        return loadDefinitionsFromTemplates(
            listOf(templateClass),
            templateClasspath,
            createHostConfiguration(gradleExeSettings, projectSettings),
            additionalResolverClasspath(gradleLibDir)
        ).map {
            it.asLegacyOrNull<KotlinScriptDefinitionFromAnnotatedTemplate>()?.let { legacyDef ->
                @Suppress("DEPRECATION")
                if (legacyDef.scriptExpectedLocations.contains(ScriptExpectedLocation.Project)) null
                else {
                    // Expand scope for old gradle script definition
                    ScriptDefinition.FromLegacy(it.hostConfiguration, GradleKotlinScriptDefinitionFromAnnotatedTemplate(legacyDef))
                }
            } ?: it
        }
    }

    fun reloadIfNeccessary() {
        if (failedToLoad.compareAndSet(true, false)) {
            reload()
        }
    }

    private fun reload() {
        ScriptDefinitionsManager.getInstance(project).reloadDefinitionsBy(this)
    }

    // TODO: refactor - minimize
    private class ErrorGradleScriptDefinition(message: String? = null) :
        ScriptDefinition.FromLegacy(ScriptingHostConfiguration(defaultJvmScriptingHostConfiguration), LegacyDefinition(message)) {

        private class LegacyDefinition(message: String?) : KotlinScriptDefinitionAdapterFromNewAPIBase() {
            companion object {
                private const val KOTLIN_DSL_SCRIPT_EXTENSION = "gradle.kts"
            }

            override val name: String = "Default Kotlin Gradle Script"
            override val fileExtension: String = KOTLIN_DSL_SCRIPT_EXTENSION

            override val scriptCompilationConfiguration: ScriptCompilationConfiguration = ScriptCompilationConfiguration.Default
            override val hostConfiguration: ScriptingHostConfiguration = ScriptingHostConfiguration(defaultJvmScriptingHostConfiguration)
            override val baseClass: KClass<*> = ScriptTemplateWithArgs::class

            override val dependencyResolver: DependenciesResolver = ErrorScriptDependenciesResolver(message)

            override fun toString(): String = "ErrorGradleScriptDefinition"
            override fun equals(other: Any?): Boolean = other is ErrorGradleScriptDefinition
            override fun hashCode(): Int = name.hashCode()
        }
    }

    private class ErrorScriptDependenciesResolver(private val message: String? = null) : DependenciesResolver {
        override fun resolve(scriptContents: ScriptContents, environment: Environment): ResolveResult {
            val failureMessage = if (ReloadGradleTemplatesOnSync.gradleState.isSyncInProgress) {
                "Highlighting is impossible during Gradle Import"
            } else {
                message ?: "Failed to load script definitions by ${GradleScriptDefinitionsContributor::class.java.name}"
            }
            return ResolveResult.Failure(ScriptReport(failureMessage, ScriptReport.Severity.FATAL))
        }
    }
}

internal class GradleSyncState {
    var isSyncInProgress: Boolean = false
}

class GradleKotlinScriptDefinitionFromAnnotatedTemplate(
    base: KotlinScriptDefinitionFromAnnotatedTemplate
) : KotlinScriptDefinitionFromAnnotatedTemplate(base.template, base.environment, base.templateClasspath) {
    @Suppress("DEPRECATION")
    override val scriptExpectedLocations: List<ScriptExpectedLocation>
        get() = listOf(ScriptExpectedLocation.Project)
}

class ReloadGradleTemplatesOnSync : ExternalSystemTaskNotificationListenerAdapter() {
    companion object {
        internal val gradleState = GradleSyncState()
    }

    override fun onStart(id: ExternalSystemTaskId, workingDir: String?) {
        if (id.type == ExternalSystemTaskType.RESOLVE_PROJECT && id.projectSystemId == GRADLE_SYSTEM_ID) {
            gradleState.isSyncInProgress = true
        }
    }

    override fun onEnd(id: ExternalSystemTaskId) {
        if (id.type == ExternalSystemTaskType.RESOLVE_PROJECT && id.projectSystemId == GRADLE_SYSTEM_ID) {
            gradleState.isSyncInProgress = false

            val project = id.findProject() ?: return
            val gradleDefinitionsContributor = ScriptDefinitionContributor.find<GradleScriptDefinitionsContributor>(project)
            gradleDefinitionsContributor?.reloadIfNeccessary()
        }
    }
}

class TopLevelSectionTokensEnumerator(script: CharSequence, identifier: String) : Enumeration<KotlinLexer> {

    private val lexer = KotlinLexer().apply {
        start(script.replace(Regex.fromLiteral("\r"), ""))
        var depth = 0

        loop@ while (tokenType != null) {
            when (tokenType) {
                KtTokens.IDENTIFIER -> if (depth == 0 && tokenText == identifier) {
                    advance()
                    skipWhiteSpaceAndComments()
                    if (tokenType == KtTokens.LBRACE)
                        break@loop
                }
                KtTokens.LBRACE -> depth += 1
                KtTokens.RBRACE -> depth -= 1
            }
            advance()
        }
    }

    private var depth = 1
    private var finished = false

    override fun hasMoreElements(): Boolean = !finished && lexer.tokenType != null

    override fun nextElement(): KotlinLexer = lexer.apply {
        advance()
        when (tokenType) {
            KtTokens.LBRACE -> depth += 1
            KtTokens.RBRACE -> {
                if (depth == 1) {
                    finished = true
                }
                depth -= 1
            }
        }
    }

    private fun KotlinLexer.skipWhiteSpaceAndComments() {
        while (tokenType in KtTokens.WHITE_SPACE_OR_COMMENT_BIT_SET) {
            advance()
        }
    }
}

fun topLevelSectionCodeTextTokens(script: CharSequence, sectionIdentifier: String): Sequence<CharSequence> =
    TopLevelSectionTokensEnumerator(script, sectionIdentifier).asSequence()
        .filter { it.tokenType !in KtTokens.WHITE_SPACE_OR_COMMENT_BIT_SET }
        .map { it.tokenSequence }