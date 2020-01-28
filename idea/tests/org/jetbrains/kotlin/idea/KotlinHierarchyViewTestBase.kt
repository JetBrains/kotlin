/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.idea

import com.intellij.ide.hierarchy.HierarchyTreeStructure
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.codeInsight.hierarchy.HierarchyViewTestFixture
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.test.JUnit3WithIdeaConfigurationRunner
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException

@RunWith(JUnit3WithIdeaConfigurationRunner::class)
abstract class KotlinHierarchyViewTestBase : KotlinLightCodeInsightFixtureTestCase() {
    private val hierarchyFixture = HierarchyViewTestFixture()

    @Throws(Exception::class)
    protected open fun doHierarchyTest(
        treeStructureComputable: Computable<out HierarchyTreeStructure?>,
        vararg fileNames: String
    ) {
        configure(fileNames)
        val expectedStructure = loadExpectedStructure()
        hierarchyFixture.doHierarchyTest(treeStructureComputable.compute(), expectedStructure)
    }

    private fun configure(fileNames: Array<String>) {
        myFixture.configureByFiles(*fileNames)
    }

    @Throws(IOException::class)
    private fun loadExpectedStructure(): String {
        val verificationFilePath = testDataPath + "/" + getTestName(false) + "_verification.xml"
        return FileUtil.loadFile(File(verificationFilePath))
    }
}