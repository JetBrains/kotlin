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

package org.jetbrains.kotlin.idea

import com.intellij.psi.NonClasspathClassFinder
import com.intellij.psi.PsiElementFinder
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.resolve.jvm.KotlinJavaPsiFacade
import org.junit.Assert

class RegisteredFindersTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor() = LightCodeInsightFixtureTestCase.JAVA_LATEST

    fun testKnownNonClasspathFinder() {
        val expectedFindersNames = setOf("GantClassFinder", "GradleClassFinder", "KotlinScriptDependenciesClassFinder", "AlternativeJreClassFinder").toMutableSet()
        val optionalFindersNames = setOf("AlternativeJreClassFinder").toMutableSet()

        project.getExtensions<PsiElementFinder>(PsiElementFinder.EP_NAME).forEach { finder ->
            if (finder is NonClasspathClassFinder) {
                val name = finder::class.java.simpleName
                val removed = expectedFindersNames.remove(name)
                Assert.assertTrue("Unknown finder found: $finder, class name: $name, search in $expectedFindersNames.\n" +
                                  "Consider updating ${KotlinJavaPsiFacade::class.java}",
                                  removed)
            }
        }
        expectedFindersNames.removeAll(optionalFindersNames)

        Assert.assertTrue("Some finders wasn't found: $expectedFindersNames", expectedFindersNames.isEmpty())
    }
}
