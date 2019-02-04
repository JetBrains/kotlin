/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.resolve

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElementFactory
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.idea.caches.project.LibraryInfo
import org.jetbrains.kotlin.idea.caches.project.getModuleInfo
import org.jetbrains.kotlin.idea.caches.project.getNullableModuleInfo
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
