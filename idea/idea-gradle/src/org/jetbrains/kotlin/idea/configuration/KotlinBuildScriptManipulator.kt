/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.ExternalLibraryDescriptor
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.cli.common.arguments.CliArgumentStringBuilder.buildArgumentString
import org.jetbrains.kotlin.cli.common.arguments.CliArgumentStringBuilder.replaceLanguageFeature
import org.jetbrains.kotlin.config.LanguageFeature
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
    private val gradleVersion = fetchGradleVersion(scriptFile)

    override fun isConfiguredWithOldSyntax(kotlinPluginName: String) = runReadAction {
        scriptFile.containsApplyKotlinPlugin(kotlinPluginName) && scriptFile.containsCompileStdLib()
    }

    override fun isConfigured(kotlinPluginExpression: String): Boolean = runReadAction {
        scriptFile.containsKotlinPluginInPluginsGroup(kotlinPluginExpression) && scriptFile.containsCompileStdLib()
    }

    override fun configureProjectBuildScript(kotlinPluginName: String, version: String): Boolean {
        if (useNewSyntax(kotlinPluginName, gradleVersion)) return false

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
        val useNewSyntax = useNewSyntax(kotlinPluginName, gradleVersion)
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

    override fun changeLanguageFeatureConfiguration(
        feature: LanguageFeature,
        state: LanguageFeature.State,
        forTests: Boolean
    ): PsiElement? =
        scriptFile.changeLanguageFeatureConfiguration(feature, state, forTests)

    override fun changeLanguageVersion(version: String, forTests: Boolean): PsiElement? =
        scriptFile.changeKotlinTaskParameter("languageVersion", version, forTests)

    override fun changeApiVersion(version: String, forTests: Boolean): PsiElement? =
        scriptFile.changeKotlinTaskParameter("apiVersion", version, forTests)

    @Suppress("OverridingDeprecatedMember")
    override fun addKotlinLibraryToModuleBuildScript(
        scope: DependencyScope,
        libraryDescriptor: ExternalLibraryDescriptor
    ) = addKotlinLibraryToModuleBuildScript(null, scope, libraryDescriptor)

    override fun addKotlinLibraryToModuleBuildScript(
        targetModule: Module?,
        scope: DependencyScope,
        libraryDescriptor: ExternalLibraryDescriptor
    ) {
        val dependencyText = getCompileDependencySnippet(
            libraryDescriptor.libraryGroupId,
            libraryDescriptor.libraryArtifactId,
            libraryDescriptor.maxVersion,
            scope.toGradleCompileScope(scriptFile.module?.getBuildSystemType() == AndroidGradle)
        )

        if (targetModule != null && usesNewMultiplatform()) {
            val findOrCreateTargetSourceSet = scriptFile
                .getKotlinBlock()
                ?.getSourceSetsBlock()
                ?.findOrCreateTargetSourceSet(targetModule.name.takeLastWhile { it != '.' })
            val dependenciesBlock = findOrCreateTargetSourceSet?.getDependenciesBlock()
            dependenciesBlock?.addExpressionIfMissing(dependencyText)
        } else {
            scriptFile.getDependenciesBlock()?.addExpressionIfMissing(dependencyText)
        }
    }

    private fun KtBlockExpression.findOrCreateTargetSourceSet(moduleName: String) =
        findTargetSourceSet(moduleName) ?: createTargetSourceSet(moduleName)

    private fun KtBlockExpression.findTargetSourceSet(moduleName: String): KtBlockExpression? = statements.find {
        it.isTargetSourceSetDeclaration(moduleName)
    }?.getOrCreateBlock()

    private fun KtExpression.getOrCreateBlock(): KtBlockExpression? = when (this) {
        is KtCallExpression -> getBlock() ?: addBlock()
        is KtReferenceExpression -> replace(KtPsiFactory(project).createExpression("$text {\n}")).cast<KtCallExpression>().getBlock()
        is KtProperty -> delegateExpressionOrInitializer?.getOrCreateBlock()
        else -> error("Impossible create block for $this")
    }

    private fun KtCallExpression.addBlock(): KtBlockExpression? = parent.addAfter(
        KtPsiFactory(project).createEmptyBody(), this
    ) as? KtBlockExpression

    private fun KtBlockExpression.createTargetSourceSet(moduleName: String) = addExpressionIfMissing("getByName(\"$moduleName\") {\n}")
        .cast<KtCallExpression>()
        .getBlock()

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
        findStdLibDependency() ?: addExpressionIfMissing(
            "implementation(${getKotlinModuleDependencySnippet(
                stdlibArtifactName,
                null
            )})"
        ) as? KtCallExpression

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
        PsiTreeUtil.findChildOfType(this, KtCallExpression::class.java)?.getBlock()

    private fun KtCallExpression.getBlock(): KtBlockExpression? =
        (valueArguments.singleOrNull()?.getArgumentExpression() as? KtLambdaExpression)?.bodyExpression
            ?: lambdaArguments.lastOrNull()?.getLambdaExpression()?.bodyExpression

    private fun KtFile.getKotlinStdlibVersion(): String? {
        return findScriptInitializer("dependencies")?.getBlock()?.let {
            when (val expression = it.findStdLibDependency()?.valueArguments?.firstOrNull()?.getArgumentExpression()) {
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

    private fun KtFile.getPluginManagementBlock(): KtBlockExpression? = findOrCreateScriptInitializer("pluginManagement", true)

    private fun KtFile.getKotlinBlock(): KtBlockExpression? = findOrCreateScriptInitializer("kotlin")

    private fun KtBlockExpression.getSourceSetsBlock(): KtBlockExpression? = findOrCreateBlock("sourceSets")

    private fun KtFile.getRepositoriesBlock(): KtBlockExpression? = findOrCreateScriptInitializer("repositories")

    private fun KtFile.getDependenciesBlock(): KtBlockExpression? = findOrCreateScriptInitializer("dependencies")

    private fun KtFile.getPluginsBlock(): KtBlockExpression? = findOrCreateScriptInitializer("plugins", true)

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
        val kotlinBlock = getKotlinBlock() ?: return null
        addImportIfMissing("org.jetbrains.kotlin.gradle.dsl.Coroutines")
        val statement = kotlinBlock.statements.find { it.text.startsWith("experimental.coroutines") }
        return if (statement != null) {
            statement.replace(psiFactory.createExpression(snippet))
        } else {
            kotlinBlock.add(psiFactory.createExpression(snippet)).apply { addNewLinesIfNeeded() }
        }
    }

    private fun KtFile.changeLanguageFeatureConfiguration(
        feature: LanguageFeature,
        state: LanguageFeature.State,
        forTests: Boolean
    ): PsiElement? {
        if (usesNewMultiplatform()) {
            state.assertApplicableInMultiplatform()
            return getKotlinBlock()
                ?.findOrCreateBlock("sourceSets")
                ?.findOrCreateBlock("all")
                ?.addExpressionIfMissing("languageSettings.enableLanguageFeature(\"${feature.name}\")")
        }

        val featureArgumentString = feature.buildArgumentString(state)
        val parameterName = "freeCompilerArgs"
        return addOrReplaceKotlinTaskParameter(
            parameterName,
            "listOf(\"$featureArgumentString\")",
            forTests
        ) {
            val newText = text.replaceLanguageFeature(feature, state, prefix = "$parameterName = listOf(", postfix = ")")
            replace(psiFactory.createExpression(newText))
        }
    }

    private fun KtFile.addOrReplaceKotlinTaskParameter(
        parameterName: String,
        defaultValue: String,
        forTests: Boolean,
        replaceIt: KtExpression.() -> PsiElement
    ): PsiElement? {
        val taskName = if (forTests) "compileTestKotlin" else "compileKotlin"
        val optionsBlock = findScriptInitializer("$taskName.kotlinOptions")?.getBlock()
        return if (optionsBlock != null) {
            val assignment = optionsBlock.statements.find {
                (it as? KtBinaryExpression)?.left?.text == parameterName
            }
            assignment?.replaceIt() ?: optionsBlock.addExpressionIfMissing("$parameterName = $defaultValue")
        } else {
            addImportIfMissing("org.jetbrains.kotlin.gradle.tasks.KotlinCompile")
            script?.blockExpression?.addDeclarationIfMissing("val $taskName: KotlinCompile by tasks")
            addTopLevelBlock("$taskName.kotlinOptions")?.addExpressionIfMissing("$parameterName = $defaultValue")
        }

    }

    private fun KtFile.changeKotlinTaskParameter(parameterName: String, parameterValue: String, forTests: Boolean): PsiElement? {
        return addOrReplaceKotlinTaskParameter(parameterName, "\"$parameterValue\"", forTests) {
            replace(psiFactory.createExpression("$parameterName = \"$parameterValue\""))
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

    private fun KtFile.getBuildScriptBlock(): KtBlockExpression? = findOrCreateScriptInitializer("buildscript", true)

    private fun KtFile.findOrCreateScriptInitializer(name: String, first: Boolean = false): KtBlockExpression? =
        findScriptInitializer(name)?.getBlock() ?: addTopLevelBlock(name, first)

    private fun KtBlockExpression.getRepositoriesBlock(): KtBlockExpression? = findOrCreateBlock("repositories")

    private fun KtBlockExpression.getDependenciesBlock(): KtBlockExpression? = findOrCreateBlock("dependencies")

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
        addAfter(psiFactory.createExpression(it), after)
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
        compileScope: String = "implementation"
    ): String {
        if (groupId != KOTLIN_GROUP_ID) {
            return "$compileScope(\"$groupId:$artifactId:$version\")"
        }

        val kotlinPluginName =
            if (scriptFile.module?.getBuildSystemType() == AndroidGradle) {
                "kotlin-android"
            } else {
                KotlinGradleModuleConfigurator.KOTLIN
            }

        if (useNewSyntax(kotlinPluginName, gradleVersion)) {
            return "$compileScope(${getKotlinModuleDependencySnippet(artifactId)})"
        }

        val libraryVersion = if (version == GSK_KOTLIN_VERSION_PROPERTY_NAME) "\$$version" else version
        return "$compileScope(${getKotlinModuleDependencySnippet(artifactId, libraryVersion)})"
    }

    companion object {
        private const val STDLIB_ARTIFACT_PREFIX = "org.jetbrains.kotlin:kotlin-stdlib"
        const val GSK_KOTLIN_VERSION_PROPERTY_NAME = "kotlin_version"

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

private fun KtExpression.isTargetSourceSetDeclaration(moduleName: String): Boolean = with(text) {
    when (this@isTargetSourceSetDeclaration) {
        is KtProperty -> startsWith("val $moduleName by") || initializer?.isTargetSourceSetDeclaration(moduleName) == true
        is KtCallExpression -> startsWith("getByName(\"$moduleName\")")
        else -> false
    }
}
