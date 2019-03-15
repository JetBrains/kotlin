/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea

import com.intellij.psi.NonClasspathClassFinder
import com.intellij.psi.PsiElementFinder
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.resolve.jvm.KotlinJavaPsiFacade
import org.junit.Assert

class RegisteredFindersTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor(): LightProjectDescriptor = LightCodeInsightFixtureTestCase.JAVA_LATEST

    fun testKnownNonClasspathFinder() {
        val expectedFindersNames = setOf(
            "GantClassFinder",
            "KotlinScriptDependenciesClassFinder"
        ).toMutableSet()

        val optionalFindersNames = setOf(
            "GradleClassFinder",
            "AlternativeJreClassFinder",
            "IdeaOpenApiClassFinder",
            "BundledGroovyClassFinder"
        )

        project.getExtensions<PsiElementFinder>(PsiElementFinder.EP_NAME).forEach { finder ->
            if (finder is NonClasspathClassFinder) {
                val name = finder::class.java.simpleName
                val isKnown = expectedFindersNames.remove(name) || optionalFindersNames.contains(name)
                Assert.assertTrue(
                    "Unknown finder found: $finder, class name: $name, search in $expectedFindersNames.\n" +
                            "Consider updating ${KotlinJavaPsiFacade::class.java}",
                    isKnown
                )
            }
        }

        expectedFindersNames.removeAll(optionalFindersNames)

        Assert.assertTrue("Some finders wasn't found: $expectedFindersNames", expectedFindersNames.isEmpty())
    }
}
