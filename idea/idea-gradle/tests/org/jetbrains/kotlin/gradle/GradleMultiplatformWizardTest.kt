/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import com.intellij.ide.projectWizard.NewProjectWizard
import com.intellij.ide.projectWizard.ProjectTypeStep
import com.intellij.ide.projectWizard.ProjectWizardTestCase
import com.intellij.ide.util.newProjectWizard.AbstractProjectWizard
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.roots.ui.configuration.ModulesProvider
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.testFramework.UsefulTestCase
import junit.framework.TestCase
import org.jetbrains.kotlin.ide.konan.gradle.KotlinGradleNativeMultiplatformModuleBuilder
import org.jetbrains.kotlin.idea.configuration.*
import org.jetbrains.kotlin.utils.PrintingLogger
import org.jetbrains.plugins.gradle.service.project.wizard.GradleModuleBuilder
import org.junit.Test
import java.io.File

class GradleMultiplatformWizardTest : ProjectWizardTestCase<AbstractProjectWizard>() {

    override fun createWizard(project: Project?, directory: File): AbstractProjectWizard {
        return NewProjectWizard(project, ModulesProvider.EMPTY_MODULES_PROVIDER, directory.path)
    }

    private fun testImportFromBuilder(
        builder: KotlinGradleAbstractMultiplatformModuleBuilder, nameRoot: String, metadataInside: Boolean = false
    ) {
        // Temporary workaround for duplicated bundled template
        class PrintingFactory : Logger.Factory {
            override fun getLoggerInstance(category: String): Logger {
                return PrintingLogger(System.out)
            }
        }
        Logger.setFactory(PrintingFactory::class.java)

        // For some reason with any other name it does not work (???)
        val projectName = "test${nameRoot}new"
        val project = createProject { step ->
            if (step is ProjectTypeStep) {
                TestCase.assertTrue(step.setSelectedTemplate("Kotlin", builder.presentableName))
                val steps = myWizard.sequence.selectedSteps
                TestCase.assertEquals(4, steps.size)
                val projectBuilder = myWizard.projectBuilder
                UsefulTestCase.assertInstanceOf(projectBuilder, builder::class.java)
                (projectBuilder as GradleModuleBuilder).name = projectName
            }
        }

        TestCase.assertEquals(projectName, project.name)
        val modules = ModuleManager.getInstance(project).modules
        TestCase.assertEquals(1, modules.size)
        val module = modules[0]
        TestCase.assertTrue(ModuleRootManager.getInstance(module).isSdkInherited)
        TestCase.assertEquals(projectName, module.name)

        val root = ProjectRootManager.getInstance(project).contentRoots[0]
        val settingsScript = VfsUtilCore.findRelativeFile("settings.gradle", root)
        TestCase.assertNotNull(settingsScript)
        TestCase.assertEquals(
            String.format("rootProject.name = '%s'\n\n", projectName) +
                    if (metadataInside) "\nenableFeaturePreview('GRADLE_METADATA')\n" else "",
            StringUtil.convertLineSeparators(VfsUtilCore.loadText(settingsScript!!))
        )

        val buildScript = VfsUtilCore.findRelativeFile("build.gradle", root)!!
        println(StringUtil.convertLineSeparators(VfsUtilCore.loadText(buildScript)))
    }

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        configureJdk()
    }

    @Test
    fun testMobile() {
        testImportFromBuilder(KotlinGradleMobileMultiplatformModuleBuilder(), "Mobile")
    }

    @Test
    fun testMobileShared() {
        testImportFromBuilder(KotlinGradleMobileSharedMultiplatformModuleBuilder(), "MobileShared", metadataInside = true)
    }

    @Test
    fun testNative() {
        testImportFromBuilder(KotlinGradleNativeMultiplatformModuleBuilder(), "Native")
    }

    @Test
    fun testShared() {
        testImportFromBuilder(KotlinGradleSharedMultiplatformModuleBuilder(), "Shared", metadataInside = true)
    }

    @Test
    fun testWeb() {
        testImportFromBuilder(KotlinGradleWebMultiplatformModuleBuilder(), "Web")
    }
}