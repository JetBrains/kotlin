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
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListenerAdapter
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.gradle.tooling.ProjectConnection
import org.jetbrains.kotlin.idea.framework.GRADLE_SYSTEM_ID
import org.jetbrains.kotlin.lexer.KotlinLexer
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import org.jetbrains.plugins.gradle.config.GradleSettingsListenerAdapter
import org.jetbrains.plugins.gradle.service.execution.GradleExecutionHelper
import org.jetbrains.plugins.gradle.settings.DistributionType
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings
import org.jetbrains.plugins.gradle.settings.GradleSettingsListener
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.script.dependencies.Environment
import kotlin.script.experimental.dependencies.ScriptDependencies


class GradleScriptDefinitionsContributor(private val project: Project): ScriptDefinitionContributor {

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

    override fun getDefinitions(): List<KotlinScriptDefinition> {
        val definitions = loadDefinitions()
        failedToLoad.set(definitions.isEmpty())
        return definitions
    }

    // NOTE: control flow here depends on suppressing exceptions from loadGradleTemplates calls
    // TODO: possibly combine exceptions from every loadGradleTemplates call, be mindful of KT-19276
    private fun loadDefinitions(): List<KotlinScriptDefinition> {
        val kotlinDslDependencySelector = Regex("^gradle-(?:kotlin-dsl|core).*\\.jar\$")
        val kotlinDslAdditionalResolverCp = ::kotlinStdlibAndCompiler
        val kotlinDslTemplates = loadGradleTemplates(
                templateClass = "org.gradle.kotlin.dsl.KotlinBuildScript",
                dependencySelector = kotlinDslDependencySelector,
                additionalResolverClasspath = kotlinDslAdditionalResolverCp

        ) + loadGradleTemplates(
                templateClass = "org.gradle.kotlin.dsl.KotlinSettingsScript",
                dependencySelector = kotlinDslDependencySelector,
                additionalResolverClasspath = kotlinDslAdditionalResolverCp
        )
        if (kotlinDslTemplates.isNotEmpty()) {
            return kotlinDslTemplates
        }
        val gradleScriptKotlinLegacyTemplates = loadGradleTemplates(
                templateClass = "org.gradle.script.lang.kotlin.KotlinBuildScript",
                dependencySelector = Regex("^gradle-(?:script-kotlin|core).*\\.jar\$"),
                additionalResolverClasspath = { emptyList() }
        )
        return gradleScriptKotlinLegacyTemplates
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
    ): List<KotlinScriptDefinition> = try {
        doLoadGradleTemplates(templateClass, dependencySelector, additionalResolverClasspath)
    }
    catch (t: Throwable) {
        // TODO: review exception handling
        emptyList()
    }


    private fun doLoadGradleTemplates(
            templateClass: String, dependencySelector: Regex,
            additionalResolverClasspath: (gradleLibDir: File) -> List<File>
    ): List<KotlinScriptDefinition> {
        fun createEnvironment(gradleExeSettings: GradleExecutionSettings): Environment {
            val gradleJvmOptions = gradleExeSettings.daemonVmOptions?.let { vmOptions ->
                CommandLineTokenizer(vmOptions).toList()
                        .mapNotNull { it?.let { it as? String } }
                        .filterNot(String::isBlank)
                        .distinct()
            } ?: emptyList()


            return mapOf(
                    "gradleHome" to gradleExeSettings.gradleHome?.let(::File),
                    "projectRoot" to (project.basePath ?: project.baseDir.canonicalPath)?.let(::File),
                    "gradleWithConnection" to { action: (ProjectConnection) -> Unit ->
                        GradleExecutionHelper().execute(project.basePath!!, null) { action(it) }
                    },
                    "gradleJavaHome" to gradleExeSettings.javaHome,
                    "gradleJvmOptions" to gradleJvmOptions,
                    "getScriptSectionTokens" to ::topLevelSectionCodeTextTokens
            )

        }

        val gradleExeSettings = ExternalSystemApiUtil.getExecutionSettings<GradleExecutionSettings>(
                project,
                ExternalSystemApiUtil.toCanonicalPath(project.basePath!!),
                GradleConstants.SYSTEM_ID)

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
                createEnvironment(gradleExeSettings),
                additionalResolverClasspath(gradleLibDir)
        )
    }

    fun reloadIfNeccessary() {
        if (failedToLoad.compareAndSet(true, false)) {
            reload()
        }
    }

    private fun reload() {
        ScriptDefinitionsManager.getInstance(project).reloadDefinitionsBy(this)
    }

}

class ReloadGradleTemplatesOnSync : ExternalSystemTaskNotificationListenerAdapter() {

    override fun onEnd(id: ExternalSystemTaskId) {
        if (id.type == ExternalSystemTaskType.RESOLVE_PROJECT && id.projectSystemId == GRADLE_SYSTEM_ID) {
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


private const val KOTLIN_BUILD_FILE_SUFFIX = ".gradle.kts"

class GradleScriptDefaultDependenciesProvider(
        private val scriptDependenciesCache: ScriptDependenciesCache
) : DefaultScriptDependenciesProvider {
    override fun defaultDependenciesFor(scriptFile: VirtualFile): ScriptDependencies? {
        if (!scriptFile.name.endsWith(KOTLIN_BUILD_FILE_SUFFIX)) return null

        return previouslyAnalyzedScriptsCombinedDependencies().takeUnless { it.classpath.isEmpty() }
    }

    private fun previouslyAnalyzedScriptsCombinedDependencies() =
            scriptDependenciesCache.combineDependencies { it.name.endsWith(KOTLIN_BUILD_FILE_SUFFIX) }
}
