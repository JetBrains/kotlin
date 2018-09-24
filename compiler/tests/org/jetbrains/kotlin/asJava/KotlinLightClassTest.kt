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

package org.jetbrains.kotlin.asJava

import com.intellij.openapi.util.Key
import com.intellij.psi.search.GlobalSearchScope
import junit.framework.TestCase
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import java.io.File

class KotlinLightClassTest : KotlinAsJavaTestBase() {
    override fun getKotlinSourceRoots(): List<File> = listOf(
            File("compiler/testData/asJava/lightClassStructure/ClassObject.kt"),
            File("compiler/testData/asJava/lightClasses/ideRegression/ImplementingMap.kt")
    )

    private val key = Key.create<String>("testKey")

    fun testClassInterchangeability() {
        val lightClass = finder.findClass("test.WithClassObject", GlobalSearchScope.allScope(project)) as KtLightClass
        val kotlinOrigin = lightClass.kotlinOrigin ?: throw AssertionError("no kotlinOrigin")
        val testValue = "some data"
        lightClass.putUserData(key, testValue)
        val anotherLightClass = kotlinOrigin.toLightClass() ?: throw AssertionError("cant get light class second time")
        TestCase.assertEquals(testValue, anotherLightClass.getUserData(key))
        TestCase.assertEquals(lightClass, anotherLightClass)
    }

    fun testMethodInterchangeability() {
        val lightClass = finder.findClass("p1.TypeHierarchyMap", GlobalSearchScope.allScope(project)) as KtLightClass
        val kotlinOrigin = lightClass.kotlinOrigin ?: throw AssertionError("no kotlinOrigin")

        val anotherLightClass = kotlinOrigin.toLightClass() ?: throw AssertionError("cant get light class second time")

        val lightMethod1 = lightClass.methods.first { it.name == "containsKey" }

        val testValue = "some data"
        lightMethod1.putUserData(key, testValue)
        val lightMethod2 = anotherLightClass.methods.first { it.name == "containsKey" }
        TestCase.assertEquals(testValue, lightMethod2.getUserData(key))
        TestCase.assertEquals(lightMethod1, lightMethod2)
    }

}