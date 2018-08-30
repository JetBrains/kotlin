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

import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.ExternalLibraryDescriptor
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.codeStyle.CodeStyleManager
import org.jetbrains.kotlin.idea.configuration.KotlinWithGradleConfigurator.Companion.getBuildScriptSettingsPsiFile
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.idea.util.module
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElementFactory
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrStatement
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrApplicationStatement
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrAssignmentExpression
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrMethodCallExpression
import org.jetbrains.plugins.groovy.lang.psi.api.util.GrStatementOwner

class GroovyBuildScriptManipulator(
    override val scriptFile: GroovyFile,
    override val preferNewSyntax: Boolean
) : GradleBuildScriptManipulator<GroovyFile> {
    override fun isConfiguredWithOldSyntax(kotlinPluginName: String): Boolean {
        val fileText = runReadAction { scriptFile.text }
        return containsDirective(fileText, getApplyPluginDirective(kotlinPluginName)) &&
                fileText.contains("org.jetbrains.kotlin") &&
                fileText.contains("kotlin-stdlib")
    }

    override fun isConfigured(kotlinPluginExpression: String): Boolean {
        val fileText = runReadAction { scriptFile.text }
        val pluginsBlockText = runReadAction { scriptFile.getBlockByName("plugins")?.text ?: "" }
        return (containsDirective(pluginsBlockText, kotlinPluginExpression)) &&
                fileText.contains("org.jetbrains.kotlin") &&
                fileText.contains("kotlin-stdlib")
    }

    override fun configureModuleBuildScript(
        kotlinPluginName: String,
        kotlinPluginExpression: String,
        stdlibArtifactName: String,
        version: String,
        jvmTarget: String?
    ): Boolean {
        val oldText = scriptFile.text

        val useNewSyntax = useNewSyntax(kotlinPluginName)
        if (useNewSyntax) {
            scriptFile
                .getPluginsBlock()
                .addLastExpressionInBlockIfNeeded("$kotlinPluginExpression version '$version'")
            scriptFile.getRepositoriesBlock().apply {
                val repository = getRepositoryForVersion(version)
                if (repository != null) {
                    scriptFile.module?.getBuildScriptSettingsPsiFile()?.let {
                        with(KotlinWithGradleConfigurator.getManipulator(it)) {
                            addPluginRepository(repository)
                            addMavenCentralPluginRepository()
                            addPluginRepository(DEFAULT_GRADLE_PLUGIN_REPOSITORY)
                        }
                    }
                }
            }
        }
        else {
            val applyPluginDirective = getGroovyApplyPluginDirective(kotlinPluginName)
            if (!containsDirective(scriptFile.text, applyPluginDirective)) {
                val apply = GroovyPsiElementFactory.getInstance(scriptFile.project).createExpressionFromText(applyPluginDirective)
                val applyStatement = getApplyStatement(scriptFile)
                if (applyStatement != null) {
                    scriptFile.addAfter(apply, applyStatement)
                } else {
                    val anchorBlock = scriptFile.getBlockByName("plugins") ?: scriptFile.getBlockByName("buildscript")
                    if (anchorBlock != null) {
                        scriptFile.addAfter(apply, anchorBlock.parent)
                    } else {
                        scriptFile.addAfter(apply, scriptFile.statements.lastOrNull() ?: scriptFile.firstChild)
                    }
                }
            }
        }

        scriptFile.getRepositoriesBlock().apply {
            addRepository(version)
            addMavenCentralIfMissing()
        }

        scriptFile.getDependenciesBlock().apply {
            addExpressionOrStatementInBlockIfNeeded(getGroovyDependencySnippet(stdlibArtifactName, !useNewSyntax), false, false)
        }

        if (jvmTarget != null) {
            changeKotlinTaskParameter(scriptFile, "jvmTarget", jvmTarget, forTests = false)
            changeKotlinTaskParameter(scriptFile, "jvmTarget", jvmTarget, forTests = true)
        }

        return scriptFile.text != oldText
    }

    override fun configureProjectBuildScript(kotlinPluginName: String, version: String): Boolean {
        if (useNewSyntax(kotlinPluginName)) return false

        val oldText = scriptFile.text
        scriptFile.apply {
            getBuildScriptBlock().apply {
                addFirstExpressionInBlockIfNeeded(VERSION.replace(VERSION_TEMPLATE, version))
            }

            getBuildScriptRepositoriesBlock().apply {
                addRepository(version)
                addMavenCentralIfMissing()
            }

            getBuildScriptDependenciesBlock().apply {
                addLastExpressionInBlockIfNeeded(CLASSPATH)
            }
        }

        return oldText != scriptFile.text
    }

    override fun changeCoroutineConfiguration(coroutineOption: String): PsiElement? {
        val snippet = "coroutines \"$coroutineOption\""
        val kotlinBlock = scriptFile.getBlockOrCreate("kotlin")
        kotlinBlock.getBlockOrCreate("experimental").apply {
            addOrReplaceExpression(snippet) { stmt ->
                (stmt as? GrMethodCall)?.invokedExpression?.text == "coroutines"
            }
        }

        return kotlinBlock.parent
    }

    override fun changeLanguageVersion(version: String, forTests: Boolean): PsiElement? =
        changeKotlinTaskParameter(scriptFile, "languageVersion", version, forTests)

    override fun changeApiVersion(version: String, forTests: Boolean): PsiElement? =
        changeKotlinTaskParameter(scriptFile, "apiVersion", version, forTests)

    override fun addKotlinLibraryToModuleBuildScript(
        scope: DependencyScope,
        libraryDescriptor: ExternalLibraryDescriptor
    ) {
        val dependencyString = String.format(
            "%s \"%s:%s:%s\"",
            scope.toGradleCompileScope(scriptFile.module?.getBuildSystemType() == AndroidGradle),
            libraryDescriptor.libraryGroupId,
            libraryDescriptor.libraryArtifactId,
            libraryDescriptor.maxVersion
        )

        scriptFile.getDependenciesBlock().apply {
            addLastExpressionInBlockIfNeeded(dependencyString)
        }
    }

    override fun getKotlinStdlibVersion(): String? {
        val versionProperty = "\$kotlin_version"
        scriptFile.getBlockByName("buildScript")?.let {
            if (it.text.contains("ext.kotlin_version = ")) {
                return versionProperty
            }
        }

        val dependencies = scriptFile.getBlockByName("dependencies")?.statements
        val stdlibArtifactPrefix = "org.jetbrains.kotlin:kotlin-stdlib:"
        dependencies?.forEach { dependency ->
            val dependencyText = dependency.text
            val startIndex = dependencyText.indexOf(stdlibArtifactPrefix) + stdlibArtifactPrefix.length
            val endIndex = dependencyText.length - 1
            if (startIndex != -1 && endIndex != -1) {
                return dependencyText.substring(startIndex, endIndex)
            }
        }

        return null
    }

    private fun addPluginRepositoryExpression(expression: String) {
        scriptFile
            .getBlockOrPrepend("pluginManagement")
            .getBlockOrCreate("repositories")
            .addLastExpressionInBlockIfNeeded(expression)
    }

    override fun addMavenCentralPluginRepository() {
        addPluginRepositoryExpression("mavenCentral()")
    }

    override fun addPluginRepository(repository: RepositoryDescription) {
        addPluginRepositoryExpression(repository.toGroovyRepositorySnippet())
    }

    override fun addResolutionStrategy(pluginId: String) {
        scriptFile
            .getBlockOrPrepend("pluginManagement")
            .getBlockOrCreate("resolutionStrategy")
            .getBlockOrCreate("eachPlugin")
            .addLastStatementInBlockIfNeeded(
                    """
                        if (requested.id.id == "$pluginId") {
                            useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:${'$'}{requested.version}")
                        }
                    """.trimIndent()
            )
    }

    private fun changeKotlinTaskParameter(
        gradleFile: GroovyFile,
        parameterName: String,
        parameterValue: String,
        forTests: Boolean
    ): PsiElement? {
        val snippet = "$parameterName = \"$parameterValue\""
        val kotlinBlock = gradleFile.getBlockOrCreate(if (forTests) "compileTestKotlin" else "compileKotlin")

        for (stmt in kotlinBlock.statements) {
            if ((stmt as? GrAssignmentExpression)?.lValue?.text == "kotlinOptions." + parameterName) {
                return stmt.replaceWithStatementFromText("kotlinOptions." + snippet)
            }
        }

        kotlinBlock.getBlockOrCreate("kotlinOptions").apply {
            addOrReplaceExpression(snippet) { stmt ->
                (stmt as? GrAssignmentExpression)?.lValue?.text == parameterName
            }
        }

        return kotlinBlock.parent
    }

    private fun getGroovyDependencySnippet(
        artifactName: String,
        withVersion: Boolean
    ) = "compile \"org.jetbrains.kotlin:$artifactName${if (withVersion) ":\$kotlin_version" else ""}\""

    private fun getApplyPluginDirective(pluginName: String) = "apply plugin: '$pluginName'"

    private fun containsDirective(fileText: String, directive: String): Boolean {
        return fileText.contains(directive)
                || fileText.contains(directive.replace("\"", "'"))
                || fileText.contains(directive.replace("'", "\""))
    }

    private fun getApplyStatement(file: GroovyFile): GrApplicationStatement? =
        file.getChildrenOfType<GrApplicationStatement>().find { it.invokedExpression.text == "apply" }

    private fun GrClosableBlock.addRepository(version: String): Boolean {
        val repository = getRepositoryForVersion(version)
        val snippet = when {
            repository != null -> repository.toGroovyRepositorySnippet()
            !isRepositoryConfigured(text) -> "$MAVEN_CENTRAL\n"
            else -> return false
        }
        return addLastExpressionInBlockIfNeeded(snippet)
    }

    private fun GrStatementOwner.getBuildScriptBlock() = getBlockOrCreate("buildscript") { newBlock ->
        val pluginsBlock = getBlockByName("plugins") ?: return@getBlockOrCreate false
        addBefore(newBlock, pluginsBlock.parent)
        true
    }

    private fun GrStatementOwner.getBuildScriptRepositoriesBlock(): GrClosableBlock =
        getBuildScriptBlock().getBlockOrCreate("repositories")

    private fun GrStatementOwner.getBuildScriptDependenciesBlock(): GrClosableBlock =
        getBuildScriptBlock().getBlockOrCreate("dependencies")

    private fun GrClosableBlock.addMavenCentralIfMissing(): Boolean =
        if (!isRepositoryConfigured(text)) addLastExpressionInBlockIfNeeded(MAVEN_CENTRAL) else false

    private fun GrStatementOwner.getRepositoriesBlock() = getBlockOrCreate("repositories")

    private fun GrStatementOwner.getDependenciesBlock(): GrClosableBlock = getBlockOrCreate("dependencies")

    private fun GrClosableBlock.addOrReplaceExpression(snippet: String, predicate: (GrStatement) -> Boolean) {
        statements.firstOrNull(predicate)?.let { stmt ->
            stmt.replaceWithStatementFromText(snippet)
            return
        }
        addLastExpressionInBlockIfNeeded(snippet)
    }

    private fun getGroovyApplyPluginDirective(pluginName: String) = "apply plugin: '$pluginName'"

    private fun GrStatement.replaceWithStatementFromText(snippet: String): GrStatement {
        val newStatement = GroovyPsiElementFactory.getInstance(project).createExpressionFromText(snippet)
        CodeStyleManager.getInstance(project).reformat(newStatement)
        return replaceWithStatement(newStatement)
    }

    companion object {
        private val VERSION_TEMPLATE = "\$VERSION$"
        private val VERSION = String.format("ext.kotlin_version = '%s'", VERSION_TEMPLATE)
        private val GRADLE_PLUGIN_ID = "kotlin-gradle-plugin"
        private val CLASSPATH = "classpath \"$KOTLIN_GROUP_ID:$GRADLE_PLUGIN_ID:\$kotlin_version\""

        private fun PsiElement.getBlockByName(name: String): GrClosableBlock? {
            return getChildrenOfType<GrMethodCallExpression>()
                .filter { it.closureArguments.isNotEmpty() }
                .find { it.invokedExpression.text == name }
                ?.let { it.closureArguments[0] }
        }

        fun GrStatementOwner.getBlockOrCreate(
            name: String,
            customInsert: GrStatementOwner.(newBlock: PsiElement) -> Boolean = { false }
        ): GrClosableBlock {
            var block = getBlockByName(name)
            if (block == null) {
                val factory = GroovyPsiElementFactory.getInstance(project)
                val newBlock = factory.createExpressionFromText("$name{\n}\n")
                if (!customInsert(newBlock)) {
                    addAfter(newBlock, statements.lastOrNull() ?: firstChild)
                }
                block = getBlockByName(name)!!
            }
            return block
        }

        fun GrStatementOwner.getBlockOrPrepend(name: String) = getBlockOrCreate(name) { newBlock ->
            addAfter(newBlock, null)
            true
        }

        fun GrStatementOwner.getPluginsBlock() = getBlockOrCreate("plugins") { newBlock ->
            addAfter(newBlock, getBlockByName("buildscript"))
            true
        }

        fun GrClosableBlock.addLastExpressionInBlockIfNeeded(expressionText: String): Boolean =
            addExpressionOrStatementInBlockIfNeeded(expressionText, false, false)

        fun GrClosableBlock.addLastStatementInBlockIfNeeded(expressionText: String): Boolean =
            addExpressionOrStatementInBlockIfNeeded(expressionText, true, false)

        private fun GrClosableBlock.addFirstExpressionInBlockIfNeeded(expressionText: String): Boolean =
            addExpressionOrStatementInBlockIfNeeded(expressionText, false, true)

        private fun GrClosableBlock.addExpressionOrStatementInBlockIfNeeded(text: String, isStatement: Boolean, isFirst: Boolean): Boolean {
            if (statements.any { StringUtil.equalsIgnoreWhitespaces(it.text, text) }) return false
            val psiFactory = GroovyPsiElementFactory.getInstance(project)
            val newStatement = if (isStatement) psiFactory.createStatementFromText(text) else psiFactory.createExpressionFromText(text)
            CodeStyleManager.getInstance(project).reformat(newStatement)
            if (!isFirst && statements.isNotEmpty()) {
                val lastStatement = statements[statements.size - 1]
                if (lastStatement != null) {
                    addAfter(newStatement, lastStatement)
                }
            } else {
                if (firstChild != null) {
                    addAfter(newStatement, firstChild)
                }
            }
            return true
        }
    }
}