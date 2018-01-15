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

package org.jetbrains.kotlin.android.lint

import com.intellij.codeInspection.InspectionProfileEntry
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl
import com.intellij.util.PathUtil
import org.jetbrains.android.inspections.lint.AndroidLintInspectionBase
import org.jetbrains.kotlin.android.KotlinAndroidTestCase
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
    }

    override fun tearDown() {
        ConfigLibraryUtil.unConfigureKotlinRuntime(myModule)
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
                }
                else if (file.isDirectory) {
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