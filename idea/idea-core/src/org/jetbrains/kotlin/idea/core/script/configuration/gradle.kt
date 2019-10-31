/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.configuration

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.source.tree.LeafPsiElement
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.kotlin.idea.core.script.configuration.cache.CachedConfigurationInputs
import org.jetbrains.kotlin.idea.core.script.configuration.listener.DefaultScriptChangeListener
import org.jetbrains.kotlin.idea.core.script.configuration.listener.ScriptConfigurationUpdater
import org.jetbrains.kotlin.idea.core.script.configuration.loader.DefaultScriptConfigurationLoader
import org.jetbrains.kotlin.idea.core.script.configuration.loader.ScriptConfigurationLoadingContext
import org.jetbrains.kotlin.idea.core.util.EDT
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtScriptInitializer
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition

class GradleScriptListener : DefaultScriptChangeListener() {
    private val listenGradleRelatedFiles = false // todo

    override fun editorActivated(file: KtFile, updater: ScriptConfigurationUpdater): Boolean {
        if (!isGradleKotlinScript(file.virtualFile)) return false
        if (listenGradleRelatedFiles) {
            updater.ensureUpToDatedConfigurationSuggested(file)
            // todo: force reload if related files changed
        } else {
            // configuration will be reloaded after editor activation, even it is already up-to-date
            // this is required for Gradle scripts, since it's classpath may depend on other files (`.properties` for example)
            updater.forceConfigurationReload(file)
        }

        return true
    }
}

class GradleScriptConfigurationLoader(project: Project) : DefaultScriptConfigurationLoader(project) {
    private val useProjectImport: Boolean
        get() = Registry.`is`("kotlin.gradle.scripts.useIdeaProjectImport", false)

    override fun shouldRunInBackground(scriptDefinition: ScriptDefinition): Boolean {
        return if (useProjectImport) false else super.shouldRunInBackground(scriptDefinition)
    }

    override fun loadDependencies(
        isFirstLoad: Boolean,
        virtualFile: VirtualFile,
        scriptDefinition: ScriptDefinition,
        context: ScriptConfigurationLoadingContext
    ): Boolean {
        if (!isGradleKotlinScript(virtualFile)) return false

        if (useProjectImport) {
            // do nothing, project import notification will be already showed
            // and configuration for gradle build scripts will be saved at the end of import
            // todo: use default configuration loader for out-of-project scripts?

            return true
        } else {
            // Gradle read files from FS
            GlobalScope.launch(EDT(project)) {
                runWriteAction {
                    FileDocumentManager.getInstance().saveAllDocuments()
                }
            }

            return super.loadDependencies(isFirstLoad, virtualFile, scriptDefinition, context)
        }
    }

    override fun getInputsStamp(file: KtFile): CachedConfigurationInputs {
        return getGradleScriptInputsStamp(project, file.virtualFile, file) ?: super.getInputsStamp(file)
    }
}

data class GradleKotlinScriptConfigurationInputs(
    val buildScriptAndPluginsSections: String
) : CachedConfigurationInputs {
    override fun isUpToDate(project: Project, file: VirtualFile, ktFile: KtFile?): Boolean {
        val actualStamp = getGradleScriptInputsStamp(project, file, ktFile)
        return actualStamp == this
    }
}

fun isGradleKotlinScript(virtualFile: VirtualFile) = virtualFile.name.endsWith(".gradle.kts")

fun getGradleScriptInputsStamp(
    project: Project,
    file: VirtualFile,
    givenKtFile: KtFile? = null
): GradleKotlinScriptConfigurationInputs? {
    if (!isGradleKotlinScript(file)) return null

    return runReadAction {
        val ktFile = givenKtFile ?: PsiManager.getInstance(project).findFile(file) as? KtFile

        if (ktFile != null) {
            val result = StringBuilder()
            ktFile.script?.blockExpression
                ?.getChildrenOfType<KtScriptInitializer>()
                ?.forEach {
                    val call = it.children.singleOrNull() as? KtCallExpression
                    val callRef = call?.firstChild?.text
                    if (callRef == "buildscript" || callRef == "plugins") {
                        result.append(callRef)
                        val lambda = call.lambdaArguments.singleOrNull()
                        lambda?.accept(object : PsiRecursiveElementVisitor(false) {
                            override fun visitElement(element: PsiElement) {
                                super.visitElement(element)
                                when (element) {
                                    is PsiWhiteSpace -> if (element.text.contains("\n")) result.append("\n")
                                    is LeafPsiElement -> result.append(element.text)
                                }
                            }
                        })
                        result.append("\n")
                    }
                }

            GradleKotlinScriptConfigurationInputs(result.toString())
        } else null
    }
}