/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.refactoring.introduce.introduceVariable

import com.intellij.ide.DataManager
import com.intellij.refactoring.RefactoringActionHandler
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import org.jetbrains.jet.JetTestCaseBuilder
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.plugin.refactoring.extractFunction.ExtractKotlinFunctionHandler
import java.io.File
import org.jetbrains.jet.JetTestUtils
import com.intellij.refactoring.BaseRefactoringProcessor.ConflictsInTestsException

public abstract class AbstractJetExtractionTest() : LightCodeInsightFixtureTestCase() {
    protected fun doIntroduceVariableTest(path: String) {
        doTest(path, KotlinIntroduceVariableHandler())
    }

    protected fun doTest(path: String, handler: RefactoringActionHandler) {
        val mainFile = File(path)
        val afterFile = File("$path.after")
        val conflictFile = File("$path.conflicts")

        myFixture.setTestDataPath("${JetTestCaseBuilder.getHomeDirectory()}/${mainFile.getParent()}")

        val file = myFixture.configureByFile(mainFile.getName()) as JetFile

        try {
            handler.invoke(
                    getProject(),
                    myFixture.getEditor(),
                    file,
                    DataManager.getInstance().getDataContext(myFixture.getEditor().getComponent())
            )

            assert(!conflictFile.exists())
            JetTestUtils.assertEqualsToFile(afterFile, file.getText()!!)
        }
        catch(e: Exception) {
            val message = if (e is ConflictsInTestsException) e.getMessages().sort().makeString(" ") else e.getMessage()
            JetTestUtils.assertEqualsToFile(conflictFile, message?.replace("\n", " ") ?: e.javaClass.getName())
        }
    }
}
