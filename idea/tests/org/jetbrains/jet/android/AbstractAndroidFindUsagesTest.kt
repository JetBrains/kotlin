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

package org.jetbrains.jet.android

import org.jetbrains.jet.plugin.PluginTestCaseBase
import kotlin.test.assertTrue
import com.intellij.codeInsight.TargetElementUtilBase
import com.intellij.codeInsight.navigation.actions.GotoDeclarationAction

public abstract class AbstractAndroidFindUsagesTest : KotlinAndroidTestCase() {

    override fun getTestDataPath() = PluginTestCaseBase.getTestDataPathBase() + "/android/findUsages/" + getTestName(true) + "/"

    public fun doTest(path: String) {
        val f = myFixture!!
        f.copyDirectoryToProject(getResDir()!!, "res")
        f.configureByFile(path + getTestName(true) + ".kt")

        val targetElement = TargetElementUtilBase.findTargetElement(f.getEditor(), TargetElementUtilBase.ELEMENT_NAME_ACCEPTED or TargetElementUtilBase.REFERENCED_ELEMENT_ACCEPTED)
        val propUsages = f.findUsages(targetElement!!)

        assertTrue(propUsages.notEmpty)
    }
}
