/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import com.intellij.framework.addSupport.FrameworkSupportInModuleConfigurable
import com.intellij.ide.util.frameworkSupport.FrameworkSupportModel
import com.intellij.openapi.externalSystem.model.project.ProjectId
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModifiableModelsProvider
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.codeStyle.CodeStyleManager
import org.jetbrains.kotlin.idea.refactoring.toPsiFile
import org.jetbrains.kotlin.psi.UserDataProperty
import org.jetbrains.plugins.gradle.frameworkSupport.GradleFrameworkSupportProvider
import org.jetbrains.plugins.gradle.service.project.wizard.GradleModuleBuilder
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElementFactory
import java.io.File

internal var Module.gradleModuleBuilder: GradleModuleBuilder? by UserDataProperty(Key.create("GRADLE_MODULE_BUILDER"))
private var Module.settingsScriptBuilder: SettingsScriptBuilder? by UserDataProperty(Key.create("SETTINGS_SCRIPT_BUILDER"))

internal fun findSettingsGradleFile(module: Module): VirtualFile? {
    val contentEntryPath = module.gradleModuleBuilder?.contentEntryPath ?: return null
    if (contentEntryPath.isEmpty()) return null
    val contentRootDir = File(contentEntryPath)
    val modelContentRootDir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(contentRootDir) ?: return null
    return modelContentRootDir.findChild(GradleConstants.SETTINGS_FILE_NAME)
            ?: module.project.baseDir.findChild(GradleConstants.SETTINGS_FILE_NAME)
}

class SettingsScriptBuilder(scriptFile: GroovyFile) {
    private val builder = StringBuilder(scriptFile.text)

    private fun findBlockBody(blockName: String, startFrom: Int = 0): Int {
        val blockOffset = builder.indexOf(blockName, startFrom)
        if (blockOffset < 0) return -1
        return builder.indexOf('{', blockOffset + 1) + 1
    }

    private fun getOrPrependTopLevelBlockBody(blockName: String): Int {
        val blockBody = findBlockBody(blockName)
        if (blockBody >= 0) return blockBody
        builder.insert(0, "$blockName {}\n")
        return findBlockBody(blockName)
    }

    private fun getOrAppendInnerBlockBody(blockName: String, offset: Int): Int {
        val repositoriesBody = findBlockBody(blockName, offset)
        if (repositoriesBody >= 0) return repositoriesBody
        builder.insert(offset, "\n$blockName {}\n")
        return findBlockBody(blockName, offset)
    }

    private fun appendExpressionToBlockIfAbsent(expression: String, offset: Int) {
        var braceCount = 1
        var blockEnd = offset
        for (i in offset..builder.lastIndex) {
            when (builder[i]) {
                '{' -> braceCount++
                '}' -> braceCount--
            }
            if (braceCount == 0) {
                blockEnd = i
                break
            }
        }
        if (!builder.substring(offset, blockEnd).contains(expression.trim())) {
            builder.insert(blockEnd, "\n$expression\n")
        }
    }

    private fun getOrCreatePluginManagementBody() = getOrPrependTopLevelBlockBody("pluginManagement")

    private fun addPluginRepositoryExpression(expression: String) {
        val repositoriesBody = getOrAppendInnerBlockBody("repositories", getOrCreatePluginManagementBody())
        appendExpressionToBlockIfAbsent(expression, repositoriesBody)
    }

    fun addMavenCentralPluginRepository() {
        addPluginRepositoryExpression("mavenCentral()")
    }

    fun addPluginRepository(repository: RepositoryDescription) {
        addPluginRepositoryExpression(repository.toGroovyRepositorySnippet())
    }

    fun addResolutionStrategy(pluginId: String) {
        val resolutionStrategyBody = getOrAppendInnerBlockBody("resolutionStrategy", getOrCreatePluginManagementBody())
        val eachPluginBody = getOrAppendInnerBlockBody("eachPlugin", resolutionStrategyBody)
        appendExpressionToBlockIfAbsent(
            """
                if (requested.id.id == "$pluginId") {
                    useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:${'$'}{requested.version}")
                }
            """.trimIndent(),
            eachPluginBody
        )
    }

    fun addIncludedModules(modules: List<String>) {
        builder.append(modules.joinToString(prefix = "include ", postfix = "\n") { "'$it'" })
    }

    fun build() = builder.toString()
}

// Circumvent write actions and modify the file directly
// TODO: Get rid of this hack when IDEA API allows manipulation of settings script similarly to the main script itself
internal fun updateSettingsScript(module: Module, updater: (SettingsScriptBuilder) -> Unit) {
    val storedSettingsBuilder = module.settingsScriptBuilder
    val settingsBuilder =
        storedSettingsBuilder
                ?: (findSettingsGradleFile(module)?.toPsiFile(module.project) as? GroovyFile)?.let { SettingsScriptBuilder(it) }
                ?: return
    if (storedSettingsBuilder == null) {
        module.settingsScriptBuilder = settingsBuilder
    }
    updater(settingsBuilder)
}

internal fun flushSettingsGradleCopy(module: Module) {
    try {
        val settingsFile = findSettingsGradleFile(module)
        val settingsScriptBuilder = module.settingsScriptBuilder
        if (settingsScriptBuilder != null && settingsFile != null) {
            val project = module.project
            val tmpFile =
                GroovyPsiElementFactory
                .getInstance(project)
                .createGroovyFile(settingsScriptBuilder.build(), false, null)
            CodeStyleManager.getInstance(project).reformat(tmpFile)
            VfsUtil.saveText(settingsFile, tmpFile.text)
        }
    } finally {
        module.gradleModuleBuilder = null
        module.settingsScriptBuilder = null
    }
}

class KotlinGradleFrameworkSupportInModuleConfigurable(
    private val model: FrameworkSupportModel,
    private val supportProvider: GradleFrameworkSupportProvider
) : FrameworkSupportInModuleConfigurable() {
    override fun createComponent() = supportProvider.createComponent()

    override fun addSupport(
        module: Module,
        rootModel: ModifiableRootModel,
        modifiableModelsProvider: ModifiableModelsProvider
    ) {
        val buildScriptData = GradleModuleBuilder.getBuildScriptData(module)
        if (buildScriptData != null) {
            val builder = model.moduleBuilder
            val projectId = (builder as? GradleModuleBuilder)?.projectId ?: ProjectId(null, module.name, null)
            try {
                module.gradleModuleBuilder = builder as? GradleModuleBuilder
                supportProvider.addSupport(projectId, module, rootModel, modifiableModelsProvider, buildScriptData)
            } finally {
                flushSettingsGradleCopy(module)
            }
        }
    }
}