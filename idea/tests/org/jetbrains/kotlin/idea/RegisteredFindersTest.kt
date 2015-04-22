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
import org.jetbrains.kotlin.idea.test.JetLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.resolve.jvm.KotlinJavaPsiFacade
import org.junit.Assert

public class RegisteredFindersTest : JetLightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor() = LightCodeInsightFixtureTestCase.JAVA_LATEST

    public fun testKnownNonClasspathFinder() {
        val expectedFindersNames = setOf("GantClassFinder", "GradleClassFinder").toMutableSet()

        getProject().getExtensions<PsiElementFinder>(PsiElementFinder.EP_NAME).forEach { finder ->
            if (finder is NonClasspathClassFinder) {
                val name = finder.javaClass.getSimpleName()
                val removed = expectedFindersNames.remove(name)
                Assert.assertTrue("Unknown finder found: $finder, class name: $name, search in $expectedFindersNames.\n" +
                                  "Consider updating ${javaClass<KotlinJavaPsiFacade>()}",
                                  removed)
            }
        }

        Assert.assertTrue("Some finders wasn't found: $expectedFindersNames", expectedFindersNames.isEmpty())
    }
}
