/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import org.gradle.tooling.ProjectConnection
import org.jetbrains.kotlin.lexer.KotlinLexer
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.script.ScriptTemplatesProvider
import org.jetbrains.plugins.gradle.service.execution.GradleExecutionHelper
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings
import java.io.File
import java.net.URLClassLoader
import java.util.*

class GradleScriptTemplatesProvider(project: Project): ScriptTemplatesProvider {

    private val gradleExeSettings: GradleExecutionSettings? by lazy {
        try {
            ExternalSystemApiUtil.getExecutionSettings<GradleExecutionSettings>(
                    project,
                    com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil.toCanonicalPath(project.basePath!!),
                    org.jetbrains.plugins.gradle.util.GradleConstants.SYSTEM_ID)
        }
        catch (e: Throwable) {
            // TODO: consider displaying the warning to the user
            Logger.getInstance(GradleScriptTemplatesProvider::class.java).warn("[kts] Cannot get gradle execution settings", e)
            null
        }
    }

    private val gradleJvmOptions: List<String> by lazy {
        gradleExeSettings?.daemonVmOptions?.let { vmOptions ->
            CommandLineTokenizer(vmOptions).toList()
                    .mapNotNull { it?.let { it as? String } }
                    .filterNot(String::isBlank)
                    .distinct()
        } ?: emptyList()
    }

    override val id: String = "Gradle"
    override val isValid: Boolean get() = gradleExeSettings?.gradleHome != null

    override val templateClassNames: Iterable<String> by lazy {
        val loader = URLClassLoader(dependenciesClasspath.map { File(it).toURI().toURL() }.toTypedArray(), ScriptTemplatesProvider::class.java.classLoader)
        val tpl = listOf("org.gradle.kotlin.dsl.KotlinBuildScript", "org.gradle.script.lang.kotlin.KotlinBuildScript").firstOrNull {
            try {
                // TODO: consider introducing caching mechanism (e.g. for ScriptDefinition) in ScriptTemplatesProvider to avoid double loading
                loader.loadClass(it)
                true
            }
            catch (e: ClassNotFoundException) { false }
            catch (e: NoClassDefFoundError) { false }
        }
        if (tpl != null) listOf(tpl) else emptyList()
    }

    override val dependenciesClasspath: Iterable<String> by lazy {
        gradleExeSettings?.gradleHome?.let { File(it, "lib") }
                ?.let { if (it.exists()) it else null }
                ?.listFiles { file -> file.extension == "jar" && depLibsPrefixes.any { file.name.startsWith(it) } }
                    ?.map { it.canonicalPath }
                ?: emptyList()
    }
    override val environment: Map<String, Any?>? by lazy {

        mapOf(
                "gradleHome" to gradleExeSettings?.gradleHome?.let(::File),
                "projectRoot" to (project.basePath ?: project.baseDir.canonicalPath)?.let(::File),
                "gradleWithConnection" to { action: (ProjectConnection) -> Unit ->
                GradleExecutionHelper().execute(project.basePath!!, null) { action(it) } },
                "gradleJavaHome" to gradleExeSettings?.javaHome,
                "gradleJvmOptions" to gradleJvmOptions,
                "getScriptSectionTokens" to ::topLevelSectionCodeTextTokens)
    }

    companion object {
        private val depLibsPrefixes = listOf("gradle-kotlin-dsl", "gradle-script-kotlin", "gradle-core")
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