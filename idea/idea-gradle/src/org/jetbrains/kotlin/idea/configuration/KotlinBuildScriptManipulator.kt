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
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.utils.addToStdlib.cast

class KotlinBuildScriptManipulator(private val kotlinScript: KtFile) : GradleBuildScriptManipulator {
    override fun isConfigured(kotlinPluginName: String): Boolean =
            runReadAction {
                kotlinScript.containsApplyKotlinPlugin(kotlinPluginName) && kotlinScript.containsCompileStdLib()
            }

    override fun configureProjectBuildScript(version: String): Boolean {
        val originalText = kotlinScript.text
        kotlinScript.getBuildScriptBlock()?.apply {
            addDeclarationIfMissing("var $GSK_KOTLIN_VERSION_PROPERTY_NAME: String by extra", true).also {
                addExpressionAfterIfMissing("$GSK_KOTLIN_VERSION_PROPERTY_NAME = \"$version\"", it)
            }

            getRepositoriesBlock()?.addRepositoryIfMissing(version)
            getDependenciesBlock()?.addPluginToClassPathIfMissing()
        }

        return originalText != kotlinScript.text
    }

    override fun configureModuleBuildScript(
            kotlinPluginName: String,
            stdlibArtifactName: String,
            version: String,
            jvmTarget: String?
    ): Boolean {
        val originalText = kotlinScript.text
        kotlinScript.apply {
            script?.blockExpression?.addDeclarationIfMissing("val $GSK_KOTLIN_VERSION_PROPERTY_NAME: String by extra", true)
            getApplyBlock()?.createPluginIfMissing(kotlinPluginName)
            getDependenciesBlock()?.addCompileStdlibIfMissing(stdlibArtifactName)
            getRepositoriesBlock()?.addRepositoryIfMissing(version)
            jvmTarget?.let {
                changeKotlinTaskParameter("jvmTarget", it, forTests = false)
                changeKotlinTaskParameter("jvmTarget", it, forTests = true)
            }
        }

        return originalText != kotlinScript.text
    }

    override fun changeCoroutineConfiguration(coroutineOption: String): PsiElement? =
            kotlinScript.changeCoroutineConfiguration(coroutineOption)

    override fun changeLanguageVersion(version: String, forTests: Boolean): PsiElement? =
            kotlinScript.changeKotlinTaskParameter("languageVersion", version, forTests)

    override fun changeApiVersion(version: String, forTests: Boolean): PsiElement? =
            kotlinScript.changeKotlinTaskParameter("apiVersion", version, forTests)

    override fun addKotlinLibraryToModuleBuildScript(
            scope: DependencyScope,
            libraryDescriptor: ExternalLibraryDescriptor,
            isAndroidModule: Boolean
    ) {
        val kotlinLibraryVersion = libraryDescriptor.maxVersion.takeIf { it != GSK_KOTLIN_VERSION_PROPERTY_NAME }
        kotlinScript.getDependenciesBlock()?.apply {
            addExpressionIfMissing(
                    getCompileDependencySnippet(
                            libraryDescriptor.libraryGroupId,
                            libraryDescriptor.libraryArtifactId,
                            scope.toGradleCompileScope(isAndroidModule),
                            kotlinLibraryVersion))
        }
    }

    override fun getKotlinStdlibVersion(): String? = kotlinScript.getKotlinStdlibVersion()

    private fun KtBlockExpression.addCompileStdlibIfMissing(stdlibArtifactName: String): KtCallExpression? =
            findCompileStdLib() ?: addExpressionIfMissing(getCompileDependencySnippet(KOTLIN_GROUP_ID, stdlibArtifactName)) as? KtCallExpression

    private fun KtFile.containsCompileStdLib(): Boolean =
            findScriptInitializer("dependencies")?.getBlock()?.findCompileStdLib() != null

    private fun KtFile.containsApplyKotlinPlugin(pluginName: String): Boolean =
            findScriptInitializer("apply")?.getBlock()?.findPlugin(pluginName) != null

    private fun KtBlockExpression.findPlugin(pluginName: String): KtCallExpression? {
        return PsiTreeUtil.getChildrenOfType(this, KtCallExpression::class.java)?.find {
            it.calleeExpression?.text == "plugin" && it.valueArguments.firstOrNull()?.text == "\"$pluginName\""
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
            val expression = it.findCompileStdLib()?.valueArguments?.firstOrNull()?.getArgumentExpression()
            when (expression) {
                is KtCallExpression -> expression.valueArguments[1].text.trim('\"')
                is KtStringTemplateExpression -> expression.text?.trim('\"')?.substringAfterLast(":")?.removePrefix("$")
                else -> null
            }
        }
    }

    private fun KtBlockExpression.findCompileStdLib(): KtCallExpression? {
        return PsiTreeUtil.getChildrenOfType(this, KtCallExpression::class.java)?.find {
            it.calleeExpression?.text == "compile" && (it.valueArguments.firstOrNull()?.getArgumentExpression()?.isKotlinStdLib() ?: false)
        }
    }

    private fun KtExpression.isKotlinStdLib(): Boolean = when (this) {
        is KtCallExpression -> calleeExpression?.text == "kotlinModule" &&
                                                        valueArguments.firstOrNull()?.getArgumentExpression()?.text?.startsWith("\"stdlib") ?: false
        is KtStringTemplateExpression -> text.startsWith("\"$STDLIB_ARTIFACT_PREFIX")
        else -> false
    }

    private fun KtFile.getRepositoriesBlock(): KtBlockExpression? =
            findScriptInitializer("repositories")?.getBlock() ?: addTopLevelBlock("repositories")

    private fun KtFile.getDependenciesBlock(): KtBlockExpression? =
            findScriptInitializer("dependencies")?.getBlock() ?: addTopLevelBlock("dependencies")

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
        }
        else {
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
            }
            else {
                optionsBlock.addExpressionIfMissing(snippet)
            }
        }
        else {
            addImportIfMissing("org.jetbrains.kotlin.gradle.tasks.KotlinCompile")
            script?.blockExpression?.addDeclarationIfMissing("val $taskName: KotlinCompile by tasks")
            addTopLevelBlock("$taskName.kotlinOptions")?.addExpressionIfMissing(snippet)
        }
    }

    private fun KtFile.getBuildScriptBlock(): KtBlockExpression? =
            findScriptInitializer("buildscript")?.getBlock() ?: addTopLevelBlock("buildscript", true)

    private fun KtBlockExpression.getRepositoriesBlock(): KtBlockExpression? =
            findBlock("repositories") ?: addBlock("repositories")

    private fun KtBlockExpression.getDependenciesBlock(): KtBlockExpression? =
            findBlock("dependencies") ?: addBlock("dependencies")

    private fun KtBlockExpression.addRepositoryIfMissing(version: String): KtCallExpression? {
        val repository = getRepositoryForVersion(version)
        val snippet = when {
            repository != null -> repository.toKotlinRepositorySnippet()
            !isRepositoryConfigured(text) -> MAVEN_CENTRAL
            else -> return null
        }

        return addExpressionIfMissing(snippet) as? KtCallExpression
    }

    private fun KtBlockExpression.addPluginToClassPathIfMissing(): KtCallExpression? =
            addExpressionIfMissing(getKotlinGradlePluginClassPathSnippet()) as? KtCallExpression

    private fun KtBlockExpression.addBlock(name: String): KtBlockExpression? {
        return add(psiFactory.createExpression("$name {\n}"))
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
            importDirectives.find { it.importPath?.pathStr == path } ?:
            importList?.add(psiFactory.createImportDirective(ImportPath.fromString(path))) as KtImportDirective

    private fun KtBlockExpression.addExpressionAfterIfMissing(text: String, after: PsiElement): KtExpression = addStatementIfMissing(text) {
        psiFactory.createExpression(it).let { created ->
            addAfter(created, after)
        }
    }

    private fun KtBlockExpression.addExpressionIfMissing(text: String, first: Boolean = false): KtExpression = addStatementIfMissing(text) {
        psiFactory.createExpression(it).let { created ->
            if(first) addAfter(created, null) else add(created)
        }
    }

    private fun KtBlockExpression.addDeclarationIfMissing(text: String, first: Boolean = false): KtDeclaration = addStatementIfMissing(text) {
        psiFactory.createDeclaration<KtDeclaration>(it).let { created ->
            if(first) addAfter(created, null) else add(created)
        }
    }

    private inline fun <reified T: PsiElement> KtBlockExpression.addStatementIfMissing(
            text: String,
            crossinline factory: (String) -> PsiElement): T {
        statements.find { it.text == text }?.let {
            return it as T
        }

        return factory(text).apply { addNewLinesIfNeeded() } as T
    }

    private fun KtPsiFactory.createScriptInitializer(text: String): KtScriptInitializer =
            createFile("dummy.kts", text).script?.blockExpression?.firstChild as KtScriptInitializer

    private val PsiElement.psiFactory: KtPsiFactory
        get() = KtPsiFactory(this)

    companion object {
        private val STDLIB_ARTIFACT_PREFIX = "org.jetbrains.kotlin:kotlin-stdlib"
        val GSK_KOTLIN_VERSION_PROPERTY_NAME = "kotlin_version"

        fun getKotlinGradlePluginClassPathSnippet(): String = "classpath(${getKotlinModuleDependencySnippet("gradle-plugin")})"

        fun getKotlinModuleDependencySnippet(artifactId: String, version: String? = null): String =
                "kotlinModule(\"${artifactId.removePrefix("kotlin-")}\", ${version?.let { "\"$it\"" } ?: GSK_KOTLIN_VERSION_PROPERTY_NAME})"

        fun getCompileDependencySnippet(groupId: String, artifactId: String, compileScope: String = "compile", version: String? = null): String =
                if (groupId == KOTLIN_GROUP_ID)
                    "$compileScope(${Companion.getKotlinModuleDependencySnippet(artifactId, version)})"
                else
                    "$compileScope(\"$groupId:$artifactId:$version\")"
    }
}
