/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.findUsages

import com.intellij.codeInsight.TargetElementUtil
import com.intellij.testFramework.UsefulTestCase
import org.jetbrains.kotlin.idea.project.PluginJetFilesProvider
import org.jetbrains.kotlin.idea.stubs.AbstractMultiModuleTest
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.test.InTextDirectivesUtils

abstract class AbstractFindUsagesMultiModuleTest : AbstractMultiModuleTest() {

    override val testPath = PluginTestCaseBase.getTestDataPathBase() + "/multiModuleFindUsages/"

    protected fun doFindUsagesTest() {
        val allFilesInProject = PluginJetFilesProvider.allFilesInProject(myProject!!)
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