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
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.gradle.tooling.ProjectConnection
import org.jetbrains.kotlin.lexer.KotlinLexer
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import org.jetbrains.plugins.gradle.service.execution.GradleExecutionHelper
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File
import java.util.*
import kotlin.script.dependencies.Environment
import kotlin.script.experimental.dependencies.ScriptDependencies

abstract class AbstractGradleScriptTemplatesProvider(
        private val project: Project,
        override val id: String,
        private val templateClass: String,
        private val dependencySelector: Regex
): ScriptDefinitionContributor {

    override fun getDefinitions(): List<KotlinScriptDefinition> {
        val gradleExeSettings = getExeSettings() ?: return emptyList()

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

    open fun additionalResolverClasspath(gradleLibDir: File): List<File> = emptyList()

    private fun getExeSettings(): GradleExecutionSettings? {
        return try {
            ExternalSystemApiUtil.getExecutionSettings(
                    project,
                    ExternalSystemApiUtil.toCanonicalPath(project.basePath!!),
                    GradleConstants.SYSTEM_ID)
        }
        catch (e: Throwable) {
            // TODO: consider displaying the warning to the user
            Logger.getInstance(AbstractGradleScriptTemplatesProvider::class.java).warn("[kts] Cannot get gradle execution settings", e)
            null
        }
    }

    private fun createEnvironment(gradleExeSettings: GradleExecutionSettings): Environment {
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
}

abstract class AbstractGradleKotlinDSLTemplateProvider(project: Project, private val templateClass: String)
    : AbstractGradleScriptTemplatesProvider(
        project,
        "Gradle Kotlin DSL",
        templateClass,
        Regex("^gradle-(?:kotlin-dsl|core).*\\.jar\$")
) {

    // TODO_R: check this against kotlin-dsl branch that uses daemon
    override fun additionalResolverClasspath(gradleLibDir: File): List<File> {
        // additionally need compiler jar to load gradle resolver
        return gradleLibDir.listFiles { file -> file.name.startsWith("kotlin-compiler-embeddable") || file.name.startsWith("kotlin-stdlib") }
                .firstOrNull()?.let(::listOf).orEmpty()

    }
}

class GradleSettingsKotlinDSLTemplateProvider(project: Project)
    : AbstractGradleKotlinDSLTemplateProvider(project, "org.gradle.kotlin.dsl.KotlinSettingsScript")

class GradleKotlinDSLTemplateProvider(project: Project)
    : AbstractGradleKotlinDSLTemplateProvider(project, "org.gradle.kotlin.dsl.KotlinBuildScript")

class LegacyGradleScriptKotlinTemplateProvider(project: Project) : AbstractGradleScriptTemplatesProvider(
        project,
        "Gradle Script Kotlin",
        "org.gradle.script.lang.kotlin.KotlinBuildScript",
        Regex("^gradle-(?:script-kotlin|core).*\\.jar\$")
)

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
        if (scriptFile.name != KOTLIN_BUILD_FILE_SUFFIX) return null

        return previouslyAnalyzedScriptsCombinedDependencies().takeUnless { it.classpath.isEmpty() }
    }

    private fun previouslyAnalyzedScriptsCombinedDependencies() =
            scriptDependenciesCache.combineDependencies { it.name.endsWith(KOTLIN_BUILD_FILE_SUFFIX) }
}
