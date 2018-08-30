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
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.configuration.KotlinWithGradleConfigurator.Companion.getBuildScriptSettingsPsiFile
import org.jetbrains.kotlin.idea.inspections.gradle.GradleHeuristicHelper.PRODUCTION_DEPENDENCY_STATEMENTS
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.idea.util.module
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.utils.addToStdlib.cast

class KotlinBuildScriptManipulator(
    override val scriptFile: KtFile,
    override val preferNewSyntax: Boolean
) : GradleBuildScriptManipulator<KtFile> {
    override fun isConfiguredWithOldSyntax(kotlinPluginName: String) = runReadAction {
        scriptFile.containsApplyKotlinPlugin(kotlinPluginName) && scriptFile.containsCompileStdLib()
    }

    override fun isConfigured(kotlinPluginExpression: String): Boolean = runReadAction {
        scriptFile.containsKotlinPluginInPluginsGroup(kotlinPluginExpression) && scriptFile.containsCompileStdLib()
    }

    override fun configureProjectBuildScript(kotlinPluginName: String, version: String): Boolean {
        if (useNewSyntax(kotlinPluginName)) return false

        val originalText = scriptFile.text
        scriptFile.getBuildScriptBlock()?.apply {
            addDeclarationIfMissing("var $GSK_KOTLIN_VERSION_PROPERTY_NAME: String by extra", true).also {
                addExpressionAfterIfMissing("$GSK_KOTLIN_VERSION_PROPERTY_NAME = \"$version\"", it)
            }

            getRepositoriesBlock()?.apply {
                addRepositoryIfMissing(version)
                addMavenCentralIfMissing()
            }

            getDependenciesBlock()?.addPluginToClassPathIfMissing()
        }

        return originalText != scriptFile.text
    }

    override fun configureModuleBuildScript(
        kotlinPluginName: String,
        kotlinPluginExpression: String,
        stdlibArtifactName: String,
        version: String,
        jvmTarget: String?
    ): Boolean {
        val originalText = scriptFile.text
        val useNewSyntax = useNewSyntax(kotlinPluginName)
        scriptFile.apply {
            if (useNewSyntax) {
                createPluginInPluginsGroupIfMissing(kotlinPluginExpression, version)
                getDependenciesBlock()?.addNoVersionCompileStdlibIfMissing(stdlibArtifactName)
                getRepositoriesBlock()?.apply {
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
            } else {
                script?.blockExpression?.addDeclarationIfMissing("val $GSK_KOTLIN_VERSION_PROPERTY_NAME: String by extra", true)
                getApplyBlock()?.createPluginIfMissing(kotlinPluginName)
                getDependenciesBlock()?.addCompileStdlibIfMissing(stdlibArtifactName)
            }
            getRepositoriesBlock()?.apply {
                addRepositoryIfMissing(version)
                addMavenCentralIfMissing()
            }
            jvmTarget?.let {
                changeKotlinTaskParameter("jvmTarget", it, forTests = false)
                changeKotlinTaskParameter("jvmTarget", it, forTests = true)
            }
        }

        return originalText != scriptFile.text
    }

    override fun changeCoroutineConfiguration(coroutineOption: String): PsiElement? =
        scriptFile.changeCoroutineConfiguration(coroutineOption)

    override fun changeLanguageVersion(version: String, forTests: Boolean): PsiElement? =
        scriptFile.changeKotlinTaskParameter("languageVersion", version, forTests)

    override fun changeApiVersion(version: String, forTests: Boolean): PsiElement? =
        scriptFile.changeKotlinTaskParameter("apiVersion", version, forTests)

    override fun addKotlinLibraryToModuleBuildScript(
        scope: DependencyScope,
        libraryDescriptor: ExternalLibraryDescriptor
    ) {
        scriptFile.getDependenciesBlock()?.apply {
            addExpressionIfMissing(
                getCompileDependencySnippet(
                    libraryDescriptor.libraryGroupId,
                    libraryDescriptor.libraryArtifactId,
                    libraryDescriptor.maxVersion,
                    scope.toGradleCompileScope(module?.getBuildSystemType() == AndroidGradle)
                )
            )
        }
    }

    override fun getKotlinStdlibVersion(): String? = scriptFile.getKotlinStdlibVersion()

    private fun KtBlockExpression.addCompileStdlibIfMissing(stdlibArtifactName: String): KtCallExpression? =
        findStdLibDependency()
                ?: addExpressionIfMissing(
                        getCompileDependencySnippet(
                                KOTLIN_GROUP_ID,
                                stdlibArtifactName,
                                version = "\$$GSK_KOTLIN_VERSION_PROPERTY_NAME"
                        )
                ) as? KtCallExpression

    private fun addPluginRepositoryExpression(expression: String) {
        scriptFile.getPluginManagementBlock()?.findOrCreateBlock("repositories")?.addExpressionIfMissing(expression)
    }

    override fun addMavenCentralPluginRepository() {
        addPluginRepositoryExpression("mavenCentral()")
    }

    override fun addPluginRepository(repository: RepositoryDescription) {
        addPluginRepositoryExpression(repository.toKotlinRepositorySnippet())
    }

    override fun addResolutionStrategy(pluginId: String) {
        scriptFile
            .getPluginManagementBlock()
            ?.findOrCreateBlock("resolutionStrategy")
            ?.findOrCreateBlock("eachPlugin")
            ?.addExpressionIfMissing(
                    """
                        if (requested.id.id == "$pluginId") {
                            useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:${'$'}{requested.version}")
                        }
                    """.trimIndent()
            )
    }

    private fun KtBlockExpression.addNoVersionCompileStdlibIfMissing(stdlibArtifactName: String): KtCallExpression? =
        findStdLibDependency() ?: addExpressionIfMissing("compile(${getKotlinModuleDependencySnippet(stdlibArtifactName, null)})") as? KtCallExpression

    private fun KtFile.containsCompileStdLib(): Boolean =
        findScriptInitializer("dependencies")?.getBlock()?.findStdLibDependency() != null

    private fun KtFile.containsApplyKotlinPlugin(pluginName: String): Boolean =
        findScriptInitializer("apply")?.getBlock()?.findPlugin(pluginName) != null

    private fun KtFile.containsKotlinPluginInPluginsGroup(pluginName: String): Boolean =
        findScriptInitializer("plugins")?.getBlock()?.findPluginInPluginsGroup(pluginName) != null

    private fun KtBlockExpression.findPlugin(pluginName: String): KtCallExpression? {
        return PsiTreeUtil.getChildrenOfType(this, KtCallExpression::class.java)?.find {
            it.calleeExpression?.text == "plugin" && it.valueArguments.firstOrNull()?.text == "\"$pluginName\""
        }
    }

    private fun KtBlockExpression.findPluginInPluginsGroup(pluginName: String): KtCallExpression? {
        return PsiTreeUtil.getChildrenOfAnyType(
                this,
                KtCallExpression::class.java,
                KtBinaryExpression::class.java,
                KtDotQualifiedExpression::class.java
        ).mapNotNull {
            when (it) {
                is KtCallExpression -> it
                is KtBinaryExpression -> {
                    if (it.operationReference.text == "version") it.left as? KtCallExpression else null
                }
                is KtDotQualifiedExpression -> {
                    if ((it.selectorExpression as? KtCallExpression)?.calleeExpression?.text == "version") {
                        it.receiverExpression as? KtCallExpression
                    } else null
                }
                else -> null
            }
        }.find {
            "${it.calleeExpression?.text?.trim() ?: ""}(${it.valueArguments.firstOrNull()?.text ?: ""})" == pluginName
        }
    }

    private fun KtFile.findScriptInitializer(startsWith: String): KtScriptInitializer? =
        PsiTreeUtil.findChildrenOfType(this, KtScriptInitializer::class.java).find { it.text.startsWith(startsWith) }

    private fun KtBlockExpression.findBlock(name: String): KtBlockExpression? {
        return getChildrenOfType<KtCallExpression>().find {
            it.calleeExpression?.text == name &&
                    it.valueArguments.singleOrNull()?.getArgumentExpression() is KtLambdaExpression
        }?.getBlock()
    }

    private fun KtScriptInitializer.getBlock(): KtBlockExpression? =
        PsiTreeUtil.findChildOfType<KtCallExpression>(this, KtCallExpression::class.java)?.getBlock()

    private fun KtCallExpression.getBlock(): KtBlockExpression? =
        (valueArguments.singleOrNull()?.getArgumentExpression() as? KtLambdaExpression)?.bodyExpression

    private fun KtFile.getKotlinStdlibVersion(): String? {
        return findScriptInitializer("dependencies")?.getBlock()?.let {
            val expression = it.findStdLibDependency()?.valueArguments?.firstOrNull()?.getArgumentExpression()
            when (expression) {
                is KtCallExpression -> expression.valueArguments.getOrNull(1)?.text?.trim('\"')
                is KtStringTemplateExpression -> expression.text?.trim('\"')?.substringAfterLast(":")?.removePrefix("$")
                else -> null
            }
        }
    }

    private fun KtBlockExpression.findStdLibDependency(): KtCallExpression? {
        return PsiTreeUtil.getChildrenOfType(this, KtCallExpression::class.java)?.find {
            val calleeText = it.calleeExpression?.text
            calleeText in PRODUCTION_DEPENDENCY_STATEMENTS && (it.valueArguments.firstOrNull()?.getArgumentExpression()?.isKotlinStdLib()
                    ?: false)
        }
    }

    private fun KtExpression.isKotlinStdLib(): Boolean = when (this) {
        is KtCallExpression -> {
            val calleeText = calleeExpression?.text
            (calleeText == "kotlinModule" || calleeText == "kotlin") &&
                    valueArguments.firstOrNull()?.getArgumentExpression()?.text?.startsWith("\"stdlib") ?: false
        }
        is KtStringTemplateExpression -> text.startsWith("\"$STDLIB_ARTIFACT_PREFIX")
        else -> false
    }

    private fun KtFile.getPluginManagementBlock(): KtBlockExpression? =
        findScriptInitializer("pluginManagement")?.getBlock() ?: addTopLevelBlock("pluginManagement", true)

    private fun KtFile.getRepositoriesBlock(): KtBlockExpression? =
        findScriptInitializer("repositories")?.getBlock() ?: addTopLevelBlock("repositories")

    private fun KtFile.getDependenciesBlock(): KtBlockExpression? =
        findScriptInitializer("dependencies")?.getBlock() ?: addTopLevelBlock("dependencies")

    private fun KtFile.getPluginsBlock(): KtBlockExpression? =
        findScriptInitializer("plugins")?.getBlock() ?: addTopLevelBlock("plugins", true)

    private fun KtFile.createPluginInPluginsGroupIfMissing(pluginName: String, version: String): KtCallExpression? =
        getPluginsBlock()?.let {
            it.findPluginInPluginsGroup(pluginName)
                    ?: it.addExpressionIfMissing("$pluginName version \"$version\"") as? KtCallExpression
        }

    private fun KtFile.createApplyBlock(): KtBlockExpression? {
        val apply = psiFactory.createScriptInitializer("apply {\n}")
        val plugins = findScriptInitializer("plugins")
        val addedElement = plugins?.addSibling(apply) ?: addToScriptBlock(apply)
        addedElement?.addNewLinesIfNeeded()
        return (addedElement as? KtScriptInitializer)?.getBlock()
    }

    private fun KtFile.getApplyBlock(): KtBlockExpression? = findScriptInitializer("apply")?.getBlock() ?: createApplyBlock()

    private fun KtBlockExpression.createPluginIfMissing(pluginName: String): KtCallExpression? =
        findPlugin(pluginName) ?: addExpressionIfMissing("plugin(\"$pluginName\")") as? KtCallExpression

    private fun KtFile.changeCoroutineConfiguration(coroutineOption: String): PsiElement? {
        val snippet = "experimental.coroutines = Coroutines.${coroutineOption.toUpperCase()}"
        val kotlinBlock = findScriptInitializer("kotlin")?.getBlock() ?: addTopLevelBlock("kotlin") ?: return null
        addImportIfMissing("org.jetbrains.kotlin.gradle.dsl.Coroutines")
        val statement = kotlinBlock.statements.find { it.text.startsWith("experimental.coroutines") }
        return if (statement != null) {
            statement.replace(psiFactory.createExpression(snippet))
        } else {
            kotlinBlock.add(psiFactory.createExpression(snippet)).apply { addNewLinesIfNeeded() }
        }
    }

    private fun KtFile.changeKotlinTaskParameter(parameterName: String, parameterValue: String, forTests: Boolean): PsiElement? {
        val snippet = "$parameterName = \"$parameterValue\""
        val taskName = if (forTests) "compileTestKotlin" else "compileKotlin"
        val optionsBlock = findScriptInitializer("$taskName.kotlinOptions")?.getBlock()
        return if (optionsBlock != null) {
            val assignment = optionsBlock.statements.find {
                (it as? KtBinaryExpression)?.left?.text == parameterName
            }
            if (assignment != null) {
                assignment.replace(psiFactory.createExpression(snippet))
            } else {
                optionsBlock.addExpressionIfMissing(snippet)
            }
        } else {
            addImportIfMissing("org.jetbrains.kotlin.gradle.tasks.KotlinCompile")
            script?.blockExpression?.addDeclarationIfMissing("val $taskName: KotlinCompile by tasks")
            addTopLevelBlock("$taskName.kotlinOptions")?.addExpressionIfMissing(snippet)
        }
    }

    private fun KtBlockExpression.getRepositorySnippet(version: String): String? {
        val repository = getRepositoryForVersion(version)
        return when {
            repository != null -> repository.toKotlinRepositorySnippet()
            !isRepositoryConfigured(text) -> MAVEN_CENTRAL
            else -> null
        }
    }

    private fun KtFile.getBuildScriptBlock(): KtBlockExpression? =
        findScriptInitializer("buildscript")?.getBlock() ?: addTopLevelBlock("buildscript", true)

    private fun KtBlockExpression.getRepositoriesBlock(): KtBlockExpression? =
        findBlock("repositories") ?: addBlock("repositories")

    private fun KtBlockExpression.getDependenciesBlock(): KtBlockExpression? =
        findBlock("dependencies") ?: addBlock("dependencies")

    private fun KtBlockExpression.addRepositoryIfMissing(version: String): KtCallExpression? {
        val snippet = getRepositorySnippet(version) ?: return null
        return addExpressionIfMissing(snippet) as? KtCallExpression
    }

    private fun KtBlockExpression.addMavenCentralIfMissing(): KtCallExpression? =
        if (!isRepositoryConfigured(text)) addExpressionIfMissing(MAVEN_CENTRAL) as? KtCallExpression else null

    private fun KtBlockExpression.findOrCreateBlock(name: String, first: Boolean = false) = findBlock(name) ?: addBlock(name, first)

    private fun KtBlockExpression.addPluginToClassPathIfMissing(): KtCallExpression? =
        addExpressionIfMissing(getKotlinGradlePluginClassPathSnippet()) as? KtCallExpression

    private fun KtBlockExpression.addBlock(name: String, first: Boolean = false): KtBlockExpression? {
        return psiFactory.createExpression("$name {\n}")
            .let { if (first) addAfter(it, null) else add(it) }
            ?.apply { addNewLinesIfNeeded() }
            ?.cast<KtCallExpression>()
            ?.getBlock()
    }

    private fun KtFile.addTopLevelBlock(name: String, first: Boolean = false): KtBlockExpression? {
        val scriptInitializer = psiFactory.createScriptInitializer("$name {\n}")
        val addedElement = addToScriptBlock(scriptInitializer, first) as? KtScriptInitializer
        addedElement?.addNewLinesIfNeeded()
        return addedElement?.getBlock()
    }

    private fun PsiElement.addSibling(element: PsiElement): PsiElement = parent.addAfter(element, this)

    private fun PsiElement.addNewLineBefore(lineBreaks: Int = 1) {
        parent.addBefore(psiFactory.createNewLine(lineBreaks), this)
    }

    private fun PsiElement.addNewLineAfter(lineBreaks: Int = 1) {
        parent.addAfter(psiFactory.createNewLine(lineBreaks), this)
    }

    private fun PsiElement.addNewLinesIfNeeded(lineBreaks: Int = 1) {
        if (prevSibling != null && prevSibling.text.isNotBlank()) {
            addNewLineBefore(lineBreaks)
        }

        if (nextSibling != null && nextSibling.text.isNotBlank()) {
            addNewLineAfter(lineBreaks)
        }
    }

    private fun KtFile.addToScriptBlock(element: PsiElement, first: Boolean = false): PsiElement? =
        if (first) script?.blockExpression?.addAfter(element, null) else script?.blockExpression?.add(element)

    private fun KtFile.addImportIfMissing(path: String): KtImportDirective =
        importDirectives.find { it.importPath?.pathStr == path } ?: importList?.add(
                psiFactory.createImportDirective(
                        ImportPath.fromString(
                                path
                        )
                )
        ) as KtImportDirective

    private fun KtBlockExpression.addExpressionAfterIfMissing(text: String, after: PsiElement): KtExpression = addStatementIfMissing(text) {
        psiFactory.createExpression(it).let { created ->
            addAfter(created, after)
        }
    }

    private fun KtBlockExpression.addExpressionIfMissing(text: String, first: Boolean = false): KtExpression = addStatementIfMissing(text) {
        psiFactory.createExpression(it).let { created ->
            if (first) addAfter(created, null) else add(created)
        }
    }

    private fun KtBlockExpression.addDeclarationIfMissing(text: String, first: Boolean = false): KtDeclaration =
        addStatementIfMissing(text) {
            psiFactory.createDeclaration<KtDeclaration>(it).let { created ->
                if (first) addAfter(created, null) else add(created)
            }
        }

    private inline fun <reified T : PsiElement> KtBlockExpression.addStatementIfMissing(
        text: String,
        crossinline factory: (String) -> PsiElement
    ): T {
        statements.find { StringUtil.equalsIgnoreWhitespaces(it.text, text) }?.let {
            return it as T
        }

        return factory(text).apply { addNewLinesIfNeeded() } as T
    }

    private fun KtPsiFactory.createScriptInitializer(text: String): KtScriptInitializer =
        createFile("dummy.kts", text).script?.blockExpression?.firstChild as KtScriptInitializer

    private val PsiElement.psiFactory: KtPsiFactory
        get() = KtPsiFactory(this)

    private fun getCompileDependencySnippet(
        groupId: String,
        artifactId: String,
        version: String?,
        compileScope: String = "compile"
    ): String {
        if (groupId != KOTLIN_GROUP_ID) {
            return "$compileScope(\"$groupId:$artifactId:$version\")"
        }

        if (useNewSyntax(if (scriptFile.module?.getBuildSystemType() == AndroidGradle) "kotlin-android" else KotlinGradleModuleConfigurator.KOTLIN)) {
            return "$compileScope(${getKotlinModuleDependencySnippet(artifactId)})"
        }

        val libraryVersion = if (version == GSK_KOTLIN_VERSION_PROPERTY_NAME) "\$$version" else version
        return "$compileScope(${getKotlinModuleDependencySnippet(artifactId, libraryVersion)})"
    }

    companion object {
        private val STDLIB_ARTIFACT_PREFIX = "org.jetbrains.kotlin:kotlin-stdlib"
        val GSK_KOTLIN_VERSION_PROPERTY_NAME = "kotlin_version"

        fun getKotlinGradlePluginClassPathSnippet(): String =
            "classpath(${getKotlinModuleDependencySnippet("gradle-plugin", "\$$GSK_KOTLIN_VERSION_PROPERTY_NAME")})"

        fun getKotlinModuleDependencySnippet(artifactId: String, version: String? = null): String {
            val moduleName = artifactId.removePrefix("kotlin-")
            return when (version) {
                null -> "kotlin(\"$moduleName\")"
                "\$$GSK_KOTLIN_VERSION_PROPERTY_NAME" -> "kotlinModule(\"$moduleName\", $GSK_KOTLIN_VERSION_PROPERTY_NAME)"
                else -> "kotlinModule(\"$moduleName\", ${"\"$version\""})"
            }
        }
    }
}
