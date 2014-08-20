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

package org.jetbrains.jet.plugin.libraries

import com.intellij.psi.PsiManager
import com.intellij.psi.impl.compiled.ClsFileImpl
import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.jet.plugin.JdkAndMockLibraryProjectDescriptor
import org.jetbrains.jet.plugin.PluginTestCaseBase
import com.intellij.testFramework.UsefulTestCase.*
import junit.framework.Assert.*
import org.jetbrains.jet.plugin.JetLightCodeInsightFixtureTestCase
import org.jetbrains.jet.plugin.JetLightProjectDescriptor
import com.intellij.openapi.util.text.StringUtil

public abstract class AbstractDecompiledTextTest() : JetLightCodeInsightFixtureTestCase() {

    private val TEST_DATA_PATH = PluginTestCaseBase.getTestDataPathBase() + "/libraries/decompiledText"

    public fun doTest(path: String) {
        val classFile = NavigateToDecompiledLibraryTest.getClassFile("test", getTestName(false), myModule!!)
        val clsFileForClassFile = PsiManager.getInstance(getProject()!!).findFile(classFile)
        assertTrue("Expecting java class file, was: " + clsFileForClassFile!!.javaClass, clsFileForClassFile is ClsFileImpl)
        val decompiledPsiFile = (clsFileForClassFile as ClsFileImpl).getDecompiledPsiFile()
        assertNotNull(decompiledPsiFile)
        assertSameLinesWithFile(path.substring(0, path.length - 1) + ".expected.kt", decompiledPsiFile!!.getText())
    }

    override fun getProjectDescriptor(): LightProjectDescriptor {
        if (isAllFilesPresentInTest()) {
            return JetLightProjectDescriptor.INSTANCE
        }
        return JdkAndMockLibraryProjectDescriptor(TEST_DATA_PATH + "/" + getTestName(false), false)
    }
}
