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

package org.jetbrains.kotlin.idea.configuration

import com.intellij.codeInsight.CodeInsightUtilCore
import com.intellij.ide.actions.OpenFileAction
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.ExternalLibraryDescriptor
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.WritingAccessProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.codeStyle.CodeStyleManager
import org.jetbrains.kotlin.idea.KotlinPluginUtil
import org.jetbrains.kotlin.idea.framework.ui.ConfigureDialogWithModulesAndVersion
import org.jetbrains.kotlin.idea.util.application.executeCommand
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.idea.versions.getStdlibArtifactId
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElementFactory
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrStatement
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrApplicationStatement
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrAssignmentExpression
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrMethodCallExpression
import org.jetbrains.plugins.groovy.lang.psi.api.util.GrStatementOwner
import java.io.File
import java.util.*

abstract class KotlinWithGradleConfigurator : KotlinProjectConfigurator {

    override fun getStatus(module: Module): ConfigureKotlinStatus {
        if (!isApplicable(module)) {
            return ConfigureKotlinStatus.NON_APPLICABLE
        }

        if (hasAnyKotlinRuntimeInScope(module)) {
            return ConfigureKotlinStatus.CONFIGURED
        }

        val buildFiles = listOf(getBuildGradleFile(module.project, getModuleFilePath(module)),
                                getBuildGradleFile(module.project, getTopLevelProjectFilePath(module.project)))
                .filterNotNull()
        if (buildFiles.none { buildFile -> allGradleConfigurators.any { it.isFileConfigured(buildFile) } })
            return ConfigureKotlinStatus.CAN_BE_CONFIGURED

        return ConfigureKotlinStatus.BROKEN
    }

    private val allGradleConfigurators: Collection<KotlinWithGradleConfigurator>
        get() = Extensions.getExtensions(KotlinProjectConfigurator.EP_NAME).filterIsInstance<KotlinWithGradleConfigurator>()

    protected open fun isApplicable(module: Module): Boolean {
        return KotlinPluginUtil.isGradleModule(module) && !KotlinPluginUtil.isAndroidGradleModule(module)
    }

    protected open fun getMinimumSupportedVersion() = "1.0.0"

    private fun isFileConfigured(projectGradleFile: GroovyFile): Boolean {
        val fileText = projectGradleFile.text
        return containsDirective(fileText, applyPluginDirective) &&
               fileText.contains("org.jetbrains.kotlin") &&
               fileText.contains("kotlin-stdlib")
    }

    @JvmSuppressWildcards
    override fun configure(project: Project, excludeModules: Collection<Module>) {
        val dialog = ConfigureDialogWithModulesAndVersion(project, this, excludeModules, getMinimumSupportedVersion())

        dialog.show()
        if (!dialog.isOK) return

        val collector = configureSilently(project, dialog.modulesToConfigure, dialog.kotlinVersion)
        collector.showNotification()
    }

    fun configureSilently(project: Project, modules: List<Module>, version: String): NotificationMessageCollector {
        return project.executeCommand("Configure Kotlin") {
            val collector = createConfigureKotlinNotificationCollector(project)
            val changedFiles = configureWithVersion(project, modules, version, collector)

            for (file in changedFiles) {
                OpenFileAction.openFile(file.virtualFile, project)
            }
            collector
        }
    }

    fun configureWithVersion(project: Project,
                             modulesToConfigure: List<Module>,
                             kotlinVersion: String,
                             collector: NotificationMessageCollector): HashSet<GroovyFile> {
        val changedFiles = HashSet<GroovyFile>()
        val projectGradleFile = getBuildGradleFile(project, getTopLevelProjectFilePath(project))
        if (projectGradleFile != null && canConfigureFile(projectGradleFile)) {
            val isModified = changeGradleFile(projectGradleFile, true, kotlinVersion, collector)
            if (isModified) {
                changedFiles.add(projectGradleFile)
            }
        }

        for (module in modulesToConfigure) {
            val file = getBuildGradleFile(project, getModuleFilePath(module))
            if (file != null && canConfigureFile(file)) {
                val isModified = changeGradleFile(file, false, kotlinVersion, collector)
                if (isModified) {
                    changedFiles.add(file)
                }
            }
            else {
                showErrorMessage(project, "Cannot find build.gradle file for module " + module.name)
            }
        }
        return changedFiles
    }

    protected fun addElementsToModuleFile(file: GroovyFile, version: String): Boolean {
        val oldText = file.text

        if (!containsDirective(file.text, applyPluginDirective)) {
            val apply = GroovyPsiElementFactory.getInstance(file.project).createExpressionFromText(applyPluginDirective)
            val applyStatement = getApplyStatement(file)
            if (applyStatement != null) {
                file.addAfter(apply, applyStatement)
            }
            else {
                val buildScript = getBlockByName(file, "buildscript")
                if (buildScript != null) {
                    file.addAfter(apply, buildScript.parent)
                }
                else {
                    file.addAfter(apply, file.statements.lastOrNull() ?: file.firstChild)
                }
            }
        }

        val repositoriesBlock = getRepositoriesBlock(file)
        addRepository(repositoriesBlock, version)

        val dependenciesBlock = getDependenciesBlock(file)
        val sdk = ModuleUtil.findModuleForPsiElement(file)?.let { ModuleRootManager.getInstance(it).sdk }
        addExpressionInBlockIfNeeded(getDependencyDirective(sdk, version), dependenciesBlock, false)
        val jvmTarget = getJvmTarget(sdk, version)
        if (jvmTarget != null) {
            changeKotlinTaskParameter(file, "jvmTarget", jvmTarget, forTests = false)
            changeKotlinTaskParameter(file, "jvmTarget", jvmTarget, forTests = true)
        }

        return file.text != oldText
    }

    protected open fun getDependencyDirective(sdk: Sdk?, version: String) = getRuntimeLibrary(sdk, version)

    protected open fun getJvmTarget(sdk: Sdk?, version: String): String? = null

    protected abstract val applyPluginDirective: String

    protected open fun addElementsToFile(
            groovyFile: GroovyFile,
            isTopLevelProjectFile: Boolean,
            version: String
    ): Boolean {
        if (!isTopLevelProjectFile) {
            var wasModified = addElementsToProjectFile(groovyFile, version)
            wasModified = wasModified or addElementsToModuleFile(groovyFile, version)
            return wasModified
        }
        return false
    }



    fun changeGradleFile(
            groovyFile: GroovyFile,
            isTopLevelProjectFile: Boolean,
            version: String,
            collector: NotificationMessageCollector
    ): Boolean {
        val isModified = groovyFile.project.executeWriteCommand("Configure build.gradle", null) {
            val isModified = addElementsToFile(groovyFile, isTopLevelProjectFile, version)

            CodeInsightUtilCore.forcePsiPostprocessAndRestoreElement(groovyFile)
            isModified
        }

        val virtualFile = groovyFile.virtualFile
        if (virtualFile != null && isModified) {
            collector.addMessage(virtualFile.path + " was modified")
        }
        return isModified
    }

    open fun getRuntimeLibrary(sdk: Sdk?, version: String): String {
        return getRuntimeLibraryForSdk(sdk, version)
    }

    companion object {
        private val VERSION_TEMPLATE = "\$VERSION$"

        val GROUP_ID = "org.jetbrains.kotlin"
        val GRADLE_PLUGIN_ID = "kotlin-gradle-plugin"

        val CLASSPATH = "classpath \"$GROUP_ID:$GRADLE_PLUGIN_ID:\$kotlin_version\""

        private val MAVEN_CENTRAL = "mavenCentral()\n"
        private val JCENTER = "jcenter()\n"

        private val VERSION = String.format("ext.kotlin_version = '%s'", VERSION_TEMPLATE)

        private fun containsDirective(fileText: String, directive: String): Boolean {
            return fileText.contains(directive)
                   || fileText.contains(directive.replace("\"", "'"))
                   || fileText.contains(directive.replace("'", "\""))
        }

        fun addKotlinLibraryToModule(module: Module, scope: DependencyScope, libraryDescriptor: ExternalLibraryDescriptor) {
            val gradleFilePath = getModuleFilePath(module)
            val gradleFile = getBuildGradleFile(module.project, gradleFilePath)

            if (gradleFile != null && canConfigureFile(gradleFile)) {
                gradleFile.project.executeWriteCommand("Add Kotlin library") {
                    val groovyScope = when (scope) {
                        DependencyScope.COMPILE -> "compile"
                        DependencyScope.TEST -> if (KotlinPluginUtil.isAndroidGradleModule(module)) {
                            // TODO we should add testCompile or androidTestCompile
                            "compile"
                        }
                        else {
                            "testCompile"
                        }
                        DependencyScope.RUNTIME -> "runtime"
                        DependencyScope.PROVIDED -> "compile"
                        else -> "compile"
                    }

                    val dependencyString = String.format(
                            "%s \"%s:%s:%s\"",
                            groovyScope, libraryDescriptor.libraryGroupId, libraryDescriptor.libraryArtifactId,
                            libraryDescriptor.maxVersion)

                    val dependenciesBlock = getDependenciesBlock(gradleFile)
                    addLastExpressionInBlockIfNeeded(dependencyString, dependenciesBlock)

                    CodeInsightUtilCore.forcePsiPostprocessAndRestoreElement(gradleFile)
                }

                val virtualFile = gradleFile.virtualFile
                if (virtualFile != null) {
                    createConfigureKotlinNotificationCollector(gradleFile.project)
                            .addMessage(virtualFile.path + " was modified")
                            .showNotification()
                }
            }
        }

        fun changeCoroutineConfiguration(module: Module, coroutineOption: String): PsiElement? {
            return changeBuildGradle(module) { gradleFile ->
                changeCoroutineConfiguration(gradleFile, coroutineOption)
            }
        }

        fun changeCoroutineConfiguration(gradleFile: GroovyFile, coroutineOption: String): PsiElement? {
            val snippet = "coroutines \"$coroutineOption\""
            val kotlinBlock = getBlockOrCreate(gradleFile, "kotlin")
            val experimentalBlock = getBlockOrCreate(kotlinBlock, "experimental")
            addOrReplaceExpression(experimentalBlock, snippet) { stmt ->
                (stmt as? GrMethodCall)?.invokedExpression?.text == "coroutines"
            }
            return kotlinBlock.parent
        }

        fun changeLanguageVersion(module: Module, languageVersion: String?, apiVersion: String? = null, forTests: Boolean): PsiElement? {
            return changeBuildGradle(module) { gradleFile ->
                var result: PsiElement? = null
                if (languageVersion != null) {
                    result = changeLanguageVersion(gradleFile, languageVersion, forTests)
                }
                if (apiVersion != null) {
                    result = changeApiVersion(gradleFile, apiVersion, forTests)
                }
                result
            }
        }

        fun changeLanguageVersion(gradleFile: GroovyFile, languageVersion: String, forTests: Boolean): PsiElement? {
            return changeKotlinTaskParameter(gradleFile, "languageVersion", languageVersion, forTests)
        }

        fun changeApiVersion(gradleFile: GroovyFile, apiVersion: String, forTests: Boolean): PsiElement? {
            return changeKotlinTaskParameter(gradleFile, "apiVersion", apiVersion, forTests)
        }

        private fun changeKotlinTaskParameter(gradleFile: GroovyFile, parameterName: String, parameterValue: String, forTests: Boolean): PsiElement? {
            val snippet = "$parameterName = \"$parameterValue\""
            val kotlinBlock = getBlockOrCreate(gradleFile, if (forTests) "compileTestKotlin" else "compileKotlin")

            for (stmt in kotlinBlock.statements) {
                if ((stmt as? GrAssignmentExpression)?.lValue?.text == "kotlinOptions." + parameterName) {
                    return stmt.replaceWithStatementFromText("kotlinOptions." + snippet)
                }
            }

            val kotlinOptionsBlock = getBlockOrCreate(kotlinBlock, "kotlinOptions")
            addOrReplaceExpression(kotlinOptionsBlock, snippet) { stmt ->
                (stmt as? GrAssignmentExpression)?.lValue?.text == parameterName
            }
            return kotlinBlock.parent
        }

        private fun changeBuildGradle(module: Module, body: (GroovyFile) -> PsiElement?): PsiElement? {
            val gradleFilePath = getModuleFilePath(module)
            val gradleFile = getBuildGradleFile(module.project, gradleFilePath)
            if (gradleFile != null && canConfigureFile(gradleFile)) {
                return gradleFile.project.executeWriteCommand("Change build.gradle configuration", null) {
                    body(gradleFile)
                }
            }
            return null
        }

        private fun addOrReplaceExpression(block: GrClosableBlock, snippet: String, predicate: (GrStatement) -> Boolean) {
            block.statements.firstOrNull(predicate)?.let { stmt ->
                stmt.replaceWithStatementFromText(snippet)
                return
            }
            addLastExpressionInBlockIfNeeded(snippet, block)
        }

        private fun GrStatement.replaceWithStatementFromText(snippet: String): GrStatement {
            val newStatement = GroovyPsiElementFactory.getInstance(project).createExpressionFromText(snippet)
            CodeStyleManager.getInstance(project).reformat(newStatement)
            return replaceWithStatement(newStatement)
        }

        fun getKotlinStdlibVersion(module: Module): String? {
            val gradleFilePath = getModuleFilePath(module)
            val gradleFile = getBuildGradleFile(module.project, gradleFilePath) ?: return null

            val versionProperty = "\$kotlin_version"
            val block = getBuildScriptBlock(gradleFile)
            if (block.text.contains("ext.kotlin_version = ")) {
                return versionProperty
            }

            val dependencies = getDependenciesBlock(gradleFile).statements
            val stdlibArtifactPrefix = "org.jetbrains.kotlin:kotlin-stdlib:"
            for (dependency in dependencies) {
                val dependencyText = dependency.text
                val startIndex = dependencyText.indexOf(stdlibArtifactPrefix) + stdlibArtifactPrefix.length
                val endIndex = dependencyText.length - 1
                if (startIndex != -1 && endIndex != -1) {
                    return dependencyText.substring(startIndex, endIndex)
                }
            }

            return null
        }

        fun addElementsToProjectFile(file: GroovyFile, version: String): Boolean {
            var wasModified: Boolean

            val buildScriptBlock = getBuildScriptBlock(file)
            wasModified = addFirstExpressionInBlockIfNeeded(VERSION.replace(VERSION_TEMPLATE, version), buildScriptBlock)

            val buildScriptRepositoriesBlock = getBuildScriptRepositoriesBlock(file)
            wasModified = wasModified or addRepository(buildScriptRepositoriesBlock, version)

            val buildScriptDependenciesBlock = getBuildScriptDependenciesBlock(file)
            wasModified = wasModified or addLastExpressionInBlockIfNeeded(CLASSPATH, buildScriptDependenciesBlock)

            return wasModified
        }

        private fun isRepositoryConfigured(repositoriesBlock: GrClosableBlock): Boolean {
            return repositoriesBlock.text.contains(MAVEN_CENTRAL) || repositoriesBlock.text.contains(JCENTER)
        }

        private fun canConfigureFile(file: GroovyFile): Boolean {
            return WritingAccessProvider.isPotentiallyWritable(file.virtualFile, null)
        }

        private fun getBuildGradleFile(project: Project, path: String?): GroovyFile? {
            if (path == null) {
                return null
            }
            val file = VfsUtil.findFileByIoFile(File(path), true) ?: return null
            val psiFile = PsiManager.getInstance(project).findFile(file) as? GroovyFile ?: return null
            return psiFile
        }

        private fun getTopLevelProjectFilePath(project: Project): String {
            return project.basePath + "/" + GradleConstants.DEFAULT_SCRIPT_NAME
        }

        private fun getModuleFilePath(module: Module): String? {
            val moduleDir = File(module.moduleFilePath).parent
            var buildGradleFile = File(moduleDir + "/" + GradleConstants.DEFAULT_SCRIPT_NAME)
            if (buildGradleFile.exists()) {
                return buildGradleFile.path
            }

            // since IDEA 145 module file is located in .idea directory
            for (file in ModuleRootManager.getInstance(module).contentRoots) {
                buildGradleFile = File(file.path + "/" + GradleConstants.DEFAULT_SCRIPT_NAME)
                if (buildGradleFile.exists()) {
                    return buildGradleFile.path
                }
            }

            val externalProjectPath = ExternalSystemApiUtil.getExternalProjectPath(module)
            if (externalProjectPath != null) {
                buildGradleFile = File(externalProjectPath + "/" + GradleConstants.DEFAULT_SCRIPT_NAME)
                if (buildGradleFile.exists()) {
                    return buildGradleFile.path
                }
            }

            return null
        }

        private fun getDependenciesBlock(file: GrStatementOwner): GrClosableBlock {
            return getBlockOrCreate(file, "dependencies")
        }

        private fun getBuildScriptBlock(file: GrStatementOwner) = getBlockOrCreate(file, "buildscript")

        private fun getBuildScriptDependenciesBlock(file: GrStatementOwner): GrClosableBlock {
            val buildScript = getBuildScriptBlock(file)
            return getBlockOrCreate(buildScript, "dependencies")
        }

        private fun getBuildScriptRepositoriesBlock(file: GrStatementOwner): GrClosableBlock {
            val buildScript = getBuildScriptBlock(file)
            return getBlockOrCreate(buildScript, "repositories")
        }

        private fun getRepositoriesBlock(file: GrStatementOwner) = getBlockOrCreate(file, "repositories")

        fun getBlockOrCreate(parent: GrStatementOwner, name: String): GrClosableBlock {
            var block = getBlockByName(parent, name)
            if (block == null) {
                val factory = GroovyPsiElementFactory.getInstance(parent.project)
                val newBlock = factory.createExpressionFromText("$name{\n}\n")
                parent.addAfter(newBlock, parent.statements.lastOrNull() ?: parent.firstChild)
                block = getBlockByName(parent, name)!!
            }
            return block
        }

        fun addLastExpressionInBlockIfNeeded(text: String, block: GrClosableBlock): Boolean {
            return addExpressionInBlockIfNeeded(text, block, false)
        }

        private fun addFirstExpressionInBlockIfNeeded(text: String, block: GrClosableBlock): Boolean {
            return addExpressionInBlockIfNeeded(text, block, true)
        }

        private fun getBlockByName(parent: PsiElement, name: String): GrClosableBlock? {
            return parent.getChildrenOfType<GrMethodCallExpression>()
                    .filter { it.closureArguments.isNotEmpty() }
                    .find { it.invokedExpression.text == name }
                    ?.let { it.closureArguments[0] }
        }

        private fun addExpressionInBlockIfNeeded(text: String, block: GrClosableBlock, isFirst: Boolean): Boolean {
            if (block.text.contains(text)) return false
            val newStatement = GroovyPsiElementFactory.getInstance(block.project).createExpressionFromText(text)
            CodeStyleManager.getInstance(block.project).reformat(newStatement)
            val statements = block.statements
            if (!isFirst && statements.isNotEmpty()) {
                val lastStatement = statements[statements.size - 1]
                if (lastStatement != null) {
                    block.addAfter(newStatement, lastStatement)
                }
            }
            else {
                val firstChild = block.firstChild
                if (firstChild != null) {
                    block.addAfter(newStatement, firstChild)
                }
            }
            return true
        }

        private fun getApplyStatement(file: GroovyFile): GrApplicationStatement? =
                file.getChildrenOfType<GrApplicationStatement>()
                        .find { it.invokedExpression.text == "apply" }

        private fun showErrorMessage(project: Project, message: String?) {
            Messages.showErrorDialog(project,
                                     "<html>Couldn't configure kotlin-gradle plugin automatically.<br/>" +
                                     (if (message != null) message + "<br/>" else "") +
                                     "<br/>See manual installation instructions <a href=\"https://kotlinlang.org/docs/reference/using-gradle.html\">here</a>.</html>",
                                     "Configure Kotlin-Gradle Plugin")
        }

        private fun addRepository(repositoriesBlock: GrClosableBlock, version: String): Boolean {
            val repository = getRepositoryForVersion(version)
            val snippet = when {
                repository != null -> repository.toRepositorySnippet()
                !isRepositoryConfigured(repositoriesBlock) -> MAVEN_CENTRAL
                else -> return false
            }
            return addLastExpressionInBlockIfNeeded(snippet, repositoriesBlock)
        }

        fun getRuntimeLibraryForSdk(sdk: Sdk?, version: String): String {
            return getDependencySnippet(getStdlibArtifactId(sdk, version))
        }

        fun getDependencySnippet(artifactId: String) = "compile \"org.jetbrains.kotlin:$artifactId:\$kotlin_version\""
    }
}
