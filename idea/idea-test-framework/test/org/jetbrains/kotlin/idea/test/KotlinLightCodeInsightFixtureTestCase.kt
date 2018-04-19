/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.test

import com.intellij.codeInsight.CodeInsightTestCase
import com.intellij.codeInsight.daemon.impl.EditorTracker
import com.intellij.ide.startup.impl.StartupManagerImpl
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.ActionManagerEx
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProviderImpl
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.ProjectScope
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.LoggedErrorProcessor
import org.apache.log4j.Logger
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.actions.internal.KotlinInternalMode
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.jetbrains.kotlin.idea.facet.configureFacet
import org.jetbrains.kotlin.idea.facet.getOrCreateFacet
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.rethrow
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.reflect.full.findAnnotation

abstract class KotlinLightCodeInsightFixtureTestCase : KotlinLightCodeInsightFixtureTestCaseBase() {
    private var kotlinInternalModeOriginalValue = false

    private val exceptions = ArrayList<Throwable>()

    protected val module: Module get() = myFixture.module

    protected open val captureExceptions = true

    override fun setUp() {
        super.setUp()
        (StartupManager.getInstance(project) as StartupManagerImpl).runPostStartupActivities()
        VfsRootAccess.allowRootAccess(KotlinTestUtils.getHomeDirectory())

        kotlinInternalModeOriginalValue = KotlinInternalMode.enabled
        KotlinInternalMode.enabled = true

        project.getComponent(EditorTracker::class.java)?.projectOpened()

        invalidateLibraryCache(project)

        if (captureExceptions) {
            LoggedErrorProcessor.setNewInstance(object : LoggedErrorProcessor() {
                override fun processError(message: String?, t: Throwable?, details: Array<out String>?, logger: Logger) {
                    exceptions.addIfNotNull(t)
                    super.processError(message, t, details, logger)
                }
            })
        }
        CodeInsightTestCase.fixTemplates()
    }

    override fun tearDown() {
        LoggedErrorProcessor.restoreDefaultProcessor()

        KotlinInternalMode.enabled = kotlinInternalModeOriginalValue
        VfsRootAccess.disallowRootAccess(KotlinTestUtils.getHomeDirectory())

        doKotlinTearDown(project) {
            super.tearDown()
        }

        if (exceptions.isNotEmpty()) {
            exceptions.forEach { it.printStackTrace() }
            throw AssertionError("Exceptions in other threads happened")
        }
    }

    override fun getProjectDescriptor(): LightProjectDescriptor
            = getProjectDescriptorFromFileDirective()

    protected fun getProjectDescriptorFromTestName(): LightProjectDescriptor {
        val testName = StringUtil.toLowerCase(getTestName(false))

        if (testName.endsWith("runtime")) {
            return KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
        }
        else if (testName.endsWith("stdlib")) {
            return ProjectDescriptorWithStdlibSources.INSTANCE
        }

        return KotlinLightProjectDescriptor.INSTANCE
    }

    protected fun getProjectDescriptorFromFileDirective(): LightProjectDescriptor {
        if (!isAllFilesPresentInTest()) {
            try {
                val fileText = FileUtil.loadFile(File(testDataPath, fileName()), true)

                val withLibraryDirective = InTextDirectivesUtils.findLinesWithPrefixesRemoved(fileText, "WITH_LIBRARY:")
                if (!withLibraryDirective.isEmpty()) {
                    return SdkAndMockLibraryProjectDescriptor(
                        PluginTestCaseBase.getTestDataPathBase() + "/" + withLibraryDirective.get(
                            0
                        ), true
                    )
                }
                else if (InTextDirectivesUtils.isDirectiveDefined(fileText, "RUNTIME_WITH_SOURCES")) {
                    return ProjectDescriptorWithStdlibSources.INSTANCE
                }
                else if (InTextDirectivesUtils.isDirectiveDefined(fileText, "RUNTIME_WITH_KOTLIN_TEST")) {
                    return KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE_WITH_KOTLIN_TEST
                }
                else if (InTextDirectivesUtils.isDirectiveDefined(fileText, "RUNTIME_WITH_FULL_JDK")) {
                    return KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE_FULL_JDK
                }
                else if (InTextDirectivesUtils.isDirectiveDefined(fileText, "RUNTIME_WITH_REFLECT")) {
                    return KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE_WITH_REFLECT
                }
                else if (InTextDirectivesUtils.isDirectiveDefined(fileText, "RUNTIME") ||
                         InTextDirectivesUtils.isDirectiveDefined(fileText, "WITH_RUNTIME")) {
                    return KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
                }
                else if (InTextDirectivesUtils.isDirectiveDefined(fileText, "JS")) {
                    return KotlinStdJSProjectDescriptor
                }
                else if (InTextDirectivesUtils.isDirectiveDefined(fileText, "ENABLE_MULTIPLATFORM")) {
                    return KotlinProjectDescriptorWithFacet.KOTLIN_STABLE_WITH_MULTIPLATFORM
                }
            }
            catch (e: IOException) {
                throw rethrow(e)
            }
        }
        return KotlinLightProjectDescriptor.INSTANCE
    }

    protected fun isAllFilesPresentInTest(): Boolean = KotlinTestUtils.isAllFilesPresentTest(getTestName(false))

    protected open fun fileName(): String
            = KotlinTestUtils.getTestDataFileName(this::class.java, this.name) ?: (getTestName(false) + ".kt")

    protected fun performNotWriteEditorAction(actionId: String): Boolean {
        val dataContext = (myFixture.editor as EditorEx).dataContext

        val managerEx = ActionManagerEx.getInstanceEx()
        val action = managerEx.getAction(actionId)
        val event = AnActionEvent(null, dataContext, ActionPlaces.UNKNOWN, Presentation(), managerEx, 0)

        action.update(event)
        if (!event.presentation.isEnabled) {
            return false
        }

        managerEx.fireBeforeActionPerformed(action, dataContext, event)
        action.actionPerformed(event)

        managerEx.fireAfterActionPerformed(action, dataContext, event)
        return true
    }

    override fun getTestDataPath(): String {
        return this::class.findAnnotation<TestMetadata>()?.value ?: super.getTestDataPath()
    }
}

fun configureCompilerOptions(fileText: String, project: Project, module: Module) {
    val version = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// LANGUAGE_VERSION: ")
    val jvmTarget = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// JVM_TARGET: ")
    val options = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// COMPILER_ARGUMENTS: ")

    if (version != null || jvmTarget != null || options != null) {
        configureLanguageAndApiVersion(project, module, version ?: LanguageVersion.LATEST_STABLE.versionString)

        val facetSettings = KotlinFacet.get(module)!!.configuration.settings

        if (jvmTarget != null) {
            (facetSettings.compilerArguments as K2JVMCompilerArguments).jvmTarget = jvmTarget
        }

        if (options != null) {
            val compilerSettings = facetSettings.compilerSettings ?: CompilerSettings().also {
                facetSettings.compilerSettings = it
            }
            compilerSettings.additionalArguments = options
            facetSettings.updateMergedArguments()
        }
    }
}

fun configureLanguageAndApiVersion(
    project: Project,
    module: Module,
    languageVersion: String,
    apiVersion: String? = null
) {
    val accessToken = WriteAction.start()
    try {
        val modelsProvider = IdeModifiableModelsProviderImpl(project)
        val facet = module.getOrCreateFacet(modelsProvider, useProjectSettings = false)
        facet.configureFacet(languageVersion, LanguageFeature.State.DISABLED, null, modelsProvider)
        if (apiVersion != null) {
            facet.configuration.settings.apiLevel = LanguageVersion.fromVersionString(apiVersion)
        }
        modelsProvider.commit()
    }
    finally {
        accessToken.finish()
    }
}

fun Project.allKotlinFiles(): List<KtFile> {
    val virtualFiles = FileTypeIndex.getFiles(KotlinFileType.INSTANCE, ProjectScope.getProjectScope(this))
    return virtualFiles
        .map { PsiManager.getInstance(this).findFile(it) }
        .filterIsInstance<KtFile>()
}

fun Project.findFileWithCaret() =
    allKotlinFiles().single { "<caret>" in VfsUtilCore.loadText(it.virtualFile) }
