/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.findUsages

import com.intellij.codeInsight.TargetElementUtil
import com.intellij.testFramework.UsefulTestCase
import org.jetbrains.kotlin.idea.stubs.AbstractMultiModuleTest
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.idea.test.allKotlinFiles
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.test.InTextDirectivesUtils

abstract class AbstractFindUsagesMultiModuleTest : AbstractMultiModuleTest() {

    override fun getTestDataPath() = PluginTestCaseBase.getTestDataPathBase() + "/multiModuleFindUsages/"

    protected fun doFindUsagesTest() {
        val allFilesInProject = project.allKotlinFiles()
        val mainFile = allFilesInProject.single { file ->
            file.text.contains("// ")
        }


        val virtualFile = mainFile.virtualFile!!
        configureByExistingFile(virtualFile)

        val mainFileName = mainFile.name
        val mainFileText = mainFile.text
        val prefix = mainFileName.substringBefore(".") + "."

        val caretElementClassNames = InTextDirectivesUtils.findLinesWithPrefixesRemoved(mainFileText, "// PSI_ELEMENT: ")

        @Suppress("UNCHECKED_CAST")
        val caretElementClass = Class.forName(caretElementClassNames.single()) as Class<out KtDeclaration>

        val parser = OptionsParser.getParserByPsiElementClass(caretElementClass)

        val rootPath = virtualFile.path.substringBeforeLast("/") + "/"

        val caretElement = TargetElementUtil.findTargetElement(myEditor, TargetElementUtil.ELEMENT_NAME_ACCEPTED)
        UsefulTestCase.assertInstanceOf(caretElement!!, caretElementClass)

        val options = parser?.parse(mainFileText, project)
        findUsagesAndCheckResults(mainFileText, prefix, rootPath, caretElement, options, project, alwaysAppendFileName = true)
    }
}