/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.android.quickfix

import com.intellij.codeInspection.InspectionProfileEntry
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.PathUtil
import org.jetbrains.android.inspections.lint.AndroidLintInspectionBase
import org.jetbrains.kotlin.android.KotlinAndroidTestCase
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import java.io.File


abstract class AbstractAndroidLintQuickfixTest : KotlinAndroidTestCase() {

    override fun setUp() {
        AndroidLintInspectionBase.invalidateInspectionShortName2IssueMap()
        super.setUp()
    }

    fun doTest(path: String) {
        val fileText = FileUtil.loadFile(File(path), true)
        val intentionText = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// INTENTION_TEXT: ") ?: error("Empty intention text")
        val mainInspectionClassName =
            InTextDirectivesUtils.findStringWithPrefixes(fileText, "// INSPECTION_CLASS: ") ?: error("Empty inspection class name")
        val dependency = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// DEPENDENCY: ")
        val intentionAvailable = !InTextDirectivesUtils.isDirectiveDefined(fileText, "// INTENTION_NOT_AVAILABLE")

        val inspection = Class.forName(mainInspectionClassName).newInstance() as InspectionProfileEntry
        myFixture.enableInspections(inspection)

        val sourceFile = myFixture.copyFileToProject(path, "src/${PathUtil.getFileName(path)}")
        myFixture.configureFromExistingVirtualFile(sourceFile)

        if (dependency != null) {
            val (dependencyFile, dependencyTargetPath) = dependency.split(" -> ").map(String::trim)
            myFixture.copyFileToProject("${PathUtil.getParentPath(path)}/$dependencyFile", "src/$dependencyTargetPath")
        }

        if (intentionAvailable) {
            val intention = myFixture.getAvailableIntention(intentionText) ?: error("Failed to find intention")
            myFixture.launchAction(intention)
            myFixture.checkResultByFile(path + ".expected")
        } else {
            assertNull("Intention should not be available", myFixture.availableIntentions.find { it.text == intentionText })
        }
    }
}