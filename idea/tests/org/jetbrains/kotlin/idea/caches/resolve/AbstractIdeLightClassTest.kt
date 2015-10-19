/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.caches.resolve

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.asJava.LightClassTestCommon
import org.jetbrains.kotlin.idea.test.JetLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.JetWithJdkAndRuntimeLightProjectDescriptor
import java.io.File

abstract class AbstractIdeLightClassTest : JetLightCodeInsightFixtureTestCase() {

    fun doTest(testDataPath: String) {
        myFixture.configureByFile(testDataPath)

        val project = project
        LightClassTestCommon.testLightClass(
                File(testDataPath),
                findLightClass = {
                    JavaPsiFacade.getInstance(project).findClass(it, GlobalSearchScope.allScope(project))
                },
                normalizeText = {
                    //NOTE: ide and compiler differ in names generated for parameters with unspecified names
                    it.replace("java.lang.String s,", "java.lang.String p,").replace("java.lang.String s)", "java.lang.String p)")
                            .replace("java.lang.String s1", "java.lang.String p1").replace("java.lang.String s2", "java.lang.String p2")
                }
        )
    }

    override fun getProjectDescriptor() = JetWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
}