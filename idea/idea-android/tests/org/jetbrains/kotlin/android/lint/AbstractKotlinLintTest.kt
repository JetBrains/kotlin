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

import org.jetbrains.android.inspections.klint.AndroidLintInspectionBase
import org.jetbrains.kotlin.android.KotlinAndroidTestCase
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil
import org.jetbrains.kotlin.test.InTextDirectivesUtils.findStringWithPrefixes
import java.io.File

abstract class AbstractKotlinLintTest : KotlinAndroidTestCase() {

    override fun setUp() {
        super.setUp()
        ConfigLibraryUtil.configureKotlinRuntime(myModule)
        AndroidLintInspectionBase.invalidateInspectionShortName2IssueMap()
        // needs access to .class files in kotlin runtime jar
        myFixture.allowTreeAccessForAllFiles()
    }

    override fun tearDown() {
        ConfigLibraryUtil.unConfigureKotlinRuntime(myModule)
        super.tearDown()
    }

    fun doTest(filename: String) {
        val ktFile = File(filename)
        val mainInspectionClassName = findStringWithPrefixes(ktFile.readText(), "// INSPECTION_CLASS: ") ?: error("Empty class name")

        val inspectionClassNames = mutableListOf(mainInspectionClassName)
        for (i in 2..100) {
            val className = findStringWithPrefixes(ktFile.readText(), "// INSPECTION_CLASS$i: ") ?: break
            inspectionClassNames += className
        }

        myFixture.enableInspections(*inspectionClassNames.map { className ->
            val inspectionClass = Class.forName(className)
            inspectionClass.newInstance() as AndroidLintInspectionBase
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

        val virtualFile = myFixture.copyFileToProject(ktFile.absolutePath, "src/" + getTestName(true) + ".kt")
        myFixture.configureFromExistingVirtualFile(virtualFile)

        myFixture.checkHighlighting(true, false, false)
    }
}