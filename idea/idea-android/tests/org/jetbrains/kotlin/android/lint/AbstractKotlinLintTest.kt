/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.android.lint

import com.intellij.codeInspection.InspectionProfileEntry
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProviderImpl
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl
import com.intellij.util.PathUtil
import org.jetbrains.android.inspections.lint.AndroidLintInspectionBase
import org.jetbrains.kotlin.android.KotlinAndroidTestCase
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.idea.facet.getOrCreateFacet
import org.jetbrains.kotlin.idea.facet.initializeIfNeeded
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.InTextDirectivesUtils.findStringWithPrefixes
import java.io.File

abstract class AbstractKotlinLintTest : KotlinAndroidTestCase() {

    override fun setUp() {
        super.setUp()
        AndroidLintInspectionBase.invalidateInspectionShortName2IssueMap()
        (myFixture as CodeInsightTestFixtureImpl).setVirtualFileFilter { false } // Allow access to tree elements.
        ConfigLibraryUtil.configureKotlinRuntime(myModule)
        ConfigLibraryUtil.addLibrary(myModule, "androidExtensionsRuntime", "dist/kotlinc/lib", arrayOf("android-extensions-runtime.jar"))

        val facet = myModule.getOrCreateFacet(IdeModifiableModelsProviderImpl(project), useProjectSettings = false, commitModel = true)

        facet.configuration.settings.apply {
            initializeIfNeeded(myModule, null)

            val arguments = CommonCompilerArguments.DummyImpl()
            arguments.pluginClasspaths = arrayOf("kotlin-android-extensions.jar")
            compilerArguments = arguments
        }
    }

    override fun tearDown() {
        ConfigLibraryUtil.unConfigureKotlinRuntime(myModule)
        ConfigLibraryUtil.removeLibrary(myModule, "androidExtensionsRuntime")
        super.tearDown()
    }

    fun doTest(path: String) {
        val ktFile = File(path)
        val fileText = ktFile.readText()
        val mainInspectionClassName = findStringWithPrefixes(fileText, "// INSPECTION_CLASS: ") ?: error("Empty class name")
        val dependencies = InTextDirectivesUtils.findLinesWithPrefixesRemoved(fileText, "// DEPENDENCY: ")

        val inspectionClassNames = mutableListOf(mainInspectionClassName)
        for (i in 2..100) {
            val className = findStringWithPrefixes(ktFile.readText(), "// INSPECTION_CLASS$i: ") ?: break
            inspectionClassNames += className
        }

        myFixture.enableInspections(*inspectionClassNames.map { className ->
            val inspectionClass = Class.forName(className)
            inspectionClass.newInstance() as InspectionProfileEntry
        }.toTypedArray())

        val additionalResourcesDir = File(ktFile.parentFile, getTestName(true))
        if (additionalResourcesDir.exists()) {
            for (file in additionalResourcesDir.listFiles()) {
                if (file.isFile) {
                    myFixture.copyFileToProject(file.absolutePath, file.name)
                } else if (file.isDirectory) {
                    myFixture.copyDirectoryToProject(file.absolutePath, file.name)
                }
            }
        }

        val virtualFile = myFixture.copyFileToProject(ktFile.absolutePath, "src/${PathUtil.getFileName(path)}")
        myFixture.configureFromExistingVirtualFile(virtualFile)

        dependencies.forEach { dependency ->
            val (dependencyFile, dependencyTargetPath) = dependency.split(" -> ").map(String::trim)
            myFixture.copyFileToProject("${PathUtil.getParentPath(path)}/$dependencyFile", "src/$dependencyTargetPath")
        }

        myFixture.checkHighlighting(true, false, true)
    }
}