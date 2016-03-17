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
import com.intellij.psi.PsiElementFactory
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.junit.Assert

class CustomModuleInfoTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    fun testModuleInfoForMembersOfLightClassForDecompiledFile() {
        //NOTE: any class with methods from stdlib will do
        val tuplesKtClass = JavaPsiFacade.getInstance(project).findClass("kotlin.TuplesKt", GlobalSearchScope.allScope(project))!!
        val classModuleInfo = tuplesKtClass.getModuleInfo()
        Assert.assertTrue(classModuleInfo is LibraryInfo)
        val methods = tuplesKtClass.methods
        Assert.assertTrue(methods.isNotEmpty())
        methods.forEach {
            Assert.assertEquals("Members of decompiled class should have the same module info", classModuleInfo, it.getModuleInfo())
        }
    }

    fun testModuleInfoForPsiCreatedByJavaPsiFactory() {
        val dummyClass = PsiElementFactory.SERVICE.getInstance(project).createClass("A")
        val moduleInfo = dummyClass.getNullableModuleInfo()
        Assert.assertEquals("Should be null for psi created by factory", null, moduleInfo)
    }
}
