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
import com.intellij.psi.PsiElement
import com.intellij.psi.codeStyle.CodeStyleManager
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

class GroovyBuildScriptManipulator(private val groovyScript: GroovyFile) : GradleBuildScriptManipulator {
    override fun isConfigured(kotlinPluginName: String): Boolean {
        val fileText = groovyScript.text
        return containsDirective(fileText, getApplyPluginDirective(kotlinPluginName)) &&
               fileText.contains("org.jetbrains.kotlin") &&
               fileText.contains("kotlin-stdlib")
    }

    override fun configureModuleBuildScript(
            kotlinPluginName: String,
            stdlibArtifactName: String,
            version: String,
            jvmTarget: String?
    ): Boolean {
        val oldText = groovyScript.text
        val applyPluginDirective = getGroovyApplyPluginDirective(kotlinPluginName)
        if (!containsDirective(groovyScript.text, applyPluginDirective)) {
            val apply = GroovyPsiElementFactory.getInstance(groovyScript.project).createExpressionFromText(applyPluginDirective)
            val applyStatement = getApplyStatement(groovyScript)
            if (applyStatement != null) {
                groovyScript.addAfter(apply, applyStatement)
            }
            else {
                val buildScriptBlock = groovyScript.getBlockByName("buildscript")
                if (buildScriptBlock != null) {
                    groovyScript.addAfter(apply, buildScriptBlock.parent)
                }
                else {
                    groovyScript.addAfter(apply, groovyScript.statements.lastOrNull() ?: groovyScript.firstChild)
                }
            }
        }

        groovyScript.getRepositoriesBlock().apply {
            addRepository(version)
        }

        groovyScript.getDependenciesBlock().apply {
            addExpressionInBlockIfNeeded(getGroovyDependencySnippet(stdlibArtifactName), false)
        }

        if (jvmTarget != null) {
            changeKotlinTaskParameter(groovyScript, "jvmTarget", jvmTarget, forTests = false)
            changeKotlinTaskParameter(groovyScript, "jvmTarget", jvmTarget, forTests = true)
        }

        return groovyScript.text != oldText
    }

    override fun configureProjectBuildScript(version: String): Boolean {
        val oldText = groovyScript.text
        groovyScript.apply {
            getBuildScriptBlock().apply {
                addFirstExpressionInBlockIfNeeded(VERSION.replace(VERSION_TEMPLATE, version))
            }

            getBuildScriptRepositoriesBlock().apply {
                addRepository(version)
            }

            getBuildScriptDependenciesBlock().apply {
                addLastExpressionInBlockIfNeeded(CLASSPATH)
            }
        }

        return oldText != groovyScript.text
    }

    override fun changeCoroutineConfiguration(coroutineOption: String): PsiElement? {
        val snippet = "coroutines \"$coroutineOption\""
        val kotlinBlock = groovyScript.getBlockOrCreate("kotlin")
        kotlinBlock.getBlockOrCreate("experimental").apply {
            addOrReplaceExpression(snippet) { stmt ->
                (stmt as? GrMethodCall)?.invokedExpression?.text == "coroutines"
            }
        }

        return kotlinBlock.parent
    }

    override fun changeLanguageVersion(version: String, forTests: Boolean): PsiElement? =
            changeKotlinTaskParameter(groovyScript, "languageVersion", version, forTests)

    override fun changeApiVersion(version: String, forTests: Boolean): PsiElement? =
            changeKotlinTaskParameter(groovyScript, "apiVersion", version, forTests)

    override fun addKotlinLibraryToModuleBuildScript(
            scope: DependencyScope,
            libraryDescriptor: ExternalLibraryDescriptor,
            isAndroidModule: Boolean
    ) {
        val dependencyString = String.format(
                "%s \"%s:%s:%s\"",
                scope.toGradleCompileScope(isAndroidModule),
                libraryDescriptor.libraryGroupId,
                libraryDescriptor.libraryArtifactId,
                libraryDescriptor.maxVersion)

        groovyScript.getDependenciesBlock().apply {
            addLastExpressionInBlockIfNeeded(dependencyString)
        }
    }

    override fun getKotlinStdlibVersion(): String? {
        val versionProperty = "\$kotlin_version"
        groovyScript.getBlockByName("buildScript")?.let {
            if (it.text.contains("ext.kotlin_version = ")) {
                return versionProperty
            }
        }

        val dependencies = groovyScript.getBlockByName("dependencies")?.statements
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

    private fun getGroovyDependencySnippet(artifactName: String) = "compile \"org.jetbrains.kotlin:$artifactName:\$kotlin_version\""

    private fun getApplyPluginDirective(pluginName: String) = "apply plugin: '$pluginName'"

    private fun containsDirective(fileText: String, directive: String): Boolean {
        return fileText.contains(directive)
               || fileText.contains(directive.replace("\"", "'"))
               || fileText.contains(directive.replace("'", "\""))
    }

    private fun getApplyStatement(file: GroovyFile): GrApplicationStatement? =
            file.getChildrenOfType<GrApplicationStatement>().find { it.invokedExpression.text == "apply" }

    private fun PsiElement.getBlockByName(name: String): GrClosableBlock? {
        return getChildrenOfType<GrMethodCallExpression>()
                .filter { it.closureArguments.isNotEmpty() }
                .find { it.invokedExpression.text == name }
                ?.let { it.closureArguments[0] }
    }

    private fun GrClosableBlock.addRepository(version: String): Boolean {
        val repository = getRepositoryForVersion(version)
        val snippet = when {
            repository != null -> repository.toGroovyRepositorySnippet()
            !isRepositoryConfigured(text) -> "$MAVEN_CENTRAL\n"
            else -> return false
        }
        return addLastExpressionInBlockIfNeeded(snippet)
    }

    private fun GrStatementOwner.getRepositoriesBlock() = getBlockOrCreate("repositories")

    private fun GrStatementOwner.getDependenciesBlock(): GrClosableBlock = getBlockOrCreate("dependencies")

    private fun GrStatementOwner.getBuildScriptBlock() = getBlockOrCreate("buildscript")

    private fun GrStatementOwner.getBuildScriptRepositoriesBlock(): GrClosableBlock =
            getBuildScriptBlock().getBlockOrCreate("repositories")

    private fun GrStatementOwner.getBuildScriptDependenciesBlock(): GrClosableBlock =
            getBuildScriptBlock().getBlockOrCreate("dependencies")

    private fun GrStatementOwner.getBlockOrCreate(name: String): GrClosableBlock {
        var block = getBlockByName(name)
        if (block == null) {
            val factory = GroovyPsiElementFactory.getInstance(project)
            val newBlock = factory.createExpressionFromText("$name{\n}\n")
            addAfter(newBlock, statements.lastOrNull() ?: firstChild)
            block = getBlockByName(name)!!
        }
        return block
    }

    private fun GrClosableBlock.addOrReplaceExpression(snippet: String, predicate: (GrStatement) -> Boolean) {
        statements.firstOrNull(predicate)?.let { stmt ->
            stmt.replaceWithStatementFromText(snippet)
            return
        }
        addLastExpressionInBlockIfNeeded(snippet)
    }

    private fun GrClosableBlock.addLastExpressionInBlockIfNeeded(expressionText: String): Boolean =
            addExpressionInBlockIfNeeded(expressionText, false)

    private fun GrClosableBlock.addFirstExpressionInBlockIfNeeded(expressionText: String): Boolean =
            addExpressionInBlockIfNeeded(expressionText, true)

    private fun GrClosableBlock.addExpressionInBlockIfNeeded(expressionText: String, isFirst: Boolean): Boolean {
        if (text.contains(expressionText)) return false
        val newStatement = GroovyPsiElementFactory.getInstance(project).createExpressionFromText(expressionText)
        CodeStyleManager.getInstance(project).reformat(newStatement)
        if (!isFirst && statements.isNotEmpty()) {
            val lastStatement = statements[statements.size - 1]
            if (lastStatement != null) {
                addAfter(newStatement, lastStatement)
            }
        }
        else {
            if (firstChild != null) {
                addAfter(newStatement, firstChild)
            }
        }
        return true
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
    }
}