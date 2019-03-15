/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.checkers

import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.FileUtilRt
import org.jetbrains.kotlin.idea.test.AstAccessControl
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.MockLibraryUtil
import java.io.File

abstract class AbstractJavaAgainstKotlinBinariesCheckerTest : AbstractJavaAgainstKotlinCheckerTest() {
    override fun setUp() {
        super.setUp()
        val testName = getTestName(false)
        if (KotlinTestUtils.isAllFilesPresentTest(testName)) {
            return
        }

        val languageLevelOption = InTextDirectivesUtils.findListWithPrefixes(configFileText ?: "", "// KOTLINC_EXTRA_OPTS")

        val libraryJar = MockLibraryUtil.compileJvmLibraryToJar(
            PluginTestCaseBase.getTestDataPathBase() + "/kotlinAndJavaChecker/javaAgainstKotlin/" + getTestName(false) + ".kt",
            "libFor$testName",
            extraOptions = languageLevelOption
        )
        val jarUrl = "jar://" + FileUtilRt.toSystemIndependentName(libraryJar.absolutePath) + "!/"
        ModuleRootModificationUtil.addModuleLibrary(module, jarUrl)
    }

    fun doTest(path: String) {
        val ktFileText = FileUtil.loadFile(File(path), true)
        val allowAstForCompiledFile = InTextDirectivesUtils.isDirectiveDefined(ktFileText, AstAccessControl.ALLOW_AST_ACCESS_DIRECTIVE)

        if (allowAstForCompiledFile) {
            allowTreeAccessForAllFiles()
        }

        doTest(true, true, path.replace(".kt", ".java"))
    }
}
