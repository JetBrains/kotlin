/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.caches

import com.intellij.openapi.projectRoots.Sdk
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.sun.tools.javac.util.Convert.shortName
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil
import org.jetbrains.kotlin.idea.test.KotlinCodeInsightTestCase
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.test.KotlinTestUtils

class KotlinShortNamesCacheTest : KotlinCodeInsightTestCase() {

    lateinit var cacheInstance: PsiShortNamesCache

    override fun setUp() {
        super.setUp()
        cacheInstance = KotlinShortNamesCache(project)
        ConfigLibraryUtil.configureKotlinRuntimeAndSdk(myModule, testProjectJdk)
    }

    override fun tearDown() {
        ConfigLibraryUtil.unConfigureKotlinRuntimeAndSdk(myModule, testProjectJdk)
        super.tearDown()

    }

    override fun getModule() = myModule!!

    fun testGetMethodsByNameIfNotMoreThanLimits() {
        val file = KotlinTestUtils.navigationMetadata("idea/testData/cache/kotlinShortNamesCacheTestData1.kt")
        configureByFile(file)
        val scope = GlobalSearchScope.allScope(project)
        assertSize(2, cacheInstance.getMethodsByNameIfNotMoreThan("foobar", scope, 2))
        assertSize(3, cacheInstance.getMethodsByNameIfNotMoreThan("foobar", scope, 3))
        assertSize(3, cacheInstance.getMethodsByNameIfNotMoreThan("foobar", scope, Int.MAX_VALUE))
    }

    fun testGetFieldsByNameIfNotMoreThanLimits() {
        val file = KotlinTestUtils.navigationMetadata("idea/testData/cache/kotlinShortNamesCacheTestData1.kt")
        configureByFile(file)
        val scope = GlobalSearchScope.allScope(project)
        assertSize(2, cacheInstance.getFieldsByNameIfNotMoreThan("barfoo", scope, 2))
        assertSize(3, cacheInstance.getFieldsByNameIfNotMoreThan("barfoo", scope, 3))
        assertSize(3, cacheInstance.getFieldsByNameIfNotMoreThan("barfoo", scope, Int.MAX_VALUE))
    }

    fun testGetAllFields() {
        val file = KotlinTestUtils.navigationMetadata("idea/testData/cache/kotlinShortNamesCacheTestData2.kt")
        configureByFile(file)
        val allFieldNameList = cacheInstance.allFieldNames.asList()
        assertContainsElements(allFieldNameList, "foobar")
        assertContainsElements(allFieldNameList, "barfoo")
    }

    fun testGetAllMethods() {
        val file = KotlinTestUtils.navigationMetadata("idea/testData/cache/kotlinShortNamesCacheTestData2.kt")
        configureByFile(file)
        val allMethodNameList = cacheInstance.allMethodNames.asList()
        assertContainsElements(allMethodNameList, "getFoobar")
        assertContainsElements(allMethodNameList, "getBarfoo")
        assertContainsElements(allMethodNameList, "setFoobar")
        assertContainsElements(allMethodNameList, "setBarfoo")
        assertContainsElements(allMethodNameList, "method1")
        assertContainsElements(allMethodNameList, "method2")
        assertContainsElements(allMethodNameList, "methodInBoth")
    }

    fun testGetAllClasses() {
        val file = KotlinTestUtils.navigationMetadata("idea/testData/cache/kotlinShortNamesCacheTestDataClasses.kt")
        configureByFile(file)
        val allClassNameList = cacheInstance.allClassNames.asList()
        assertContainsElements(allClassNameList, "C1")
        assertContainsElements(allClassNameList, "O1")
    }

    fun methodArrayDebugToString(a: Array<PsiMethod>)
            = a.map { "${(it as KtLightMethod).clsDelegate.getKotlinFqName()} static=${it.hasModifierProperty(PsiModifier.STATIC)}" }.joinToString("\n")

    fun accessorArrayDebugToString(a: Array<PsiMethod>)
            = a.map { "${(it as KtLightMethod).clsDelegate.getKotlinFqName()} property=${(it.lightMethodOrigin?.originalElement as KtProperty).fqName} static=${it.hasModifierProperty(PsiModifier.STATIC)}" }.joinToString("\n")

    fun checkMethodFound(methods: Array<PsiMethod>, stringFqName: String, static: Boolean) {
        assertNotNull("Method $stringFqName with static=$static not found\n" + methodArrayDebugToString(methods),
                      methods.find {
                          stringFqName == (it as KtLightMethod).clsDelegate.getKotlinFqName().toString()
                          &&
                          it.hasModifierProperty(PsiModifier.STATIC) == static
                      })
    }

    fun checkIsSingleMethodFound(scope: GlobalSearchScope, stringFqName: String, static: Boolean, query: String = shortName(stringFqName)) {
        cacheInstance.getMethodsByName(query, scope).let {
            checkMethodFound(it, stringFqName, static)
            assertSize(1, it)
        }
    }

    fun checkIsSingleMethodFoundCompanion(scope: GlobalSearchScope, delegateFqName: String, originalFqName: String, query: String = shortName(originalFqName)) {
        cacheInstance.getMethodsByName(query, scope).let {
            checkMethodFound(it, delegateFqName, true)
            checkMethodFound(it, originalFqName, false)
            assertEquals((it[0] as KtLightMethod).kotlinOrigin, (it[1] as KtLightMethod).kotlinOrigin)
            assertSize(2, it)
        }
    }

    fun checkIsVarAccessorsFound(scope: GlobalSearchScope, stringVarFqName: String, getFqName: String, setFqName: String, static: Boolean) {
        val varName = shortName(stringVarFqName)

        cacheInstance.getMethodsByName(JvmAbi.getterName(varName), scope).let {
            checkAccessorFound(it, getFqName, stringVarFqName, static)
            assertSize(1, it)
        }
        cacheInstance.getMethodsByName(JvmAbi.setterName(varName), scope).let {
            checkAccessorFound(it, setFqName, stringVarFqName, static)
            assertSize(1, it)
        }
    }

    fun checkIsVarAccessorsFound(scope: GlobalSearchScope, stringVarFqName: String, static: Boolean) {
        val (getter, setter) = accessorsFqNameStringFor(stringVarFqName)
        checkIsVarAccessorsFound(scope, stringVarFqName, getter, setter, static)
    }

    fun checkIsVarAccessorsFoundCompanion(scope: GlobalSearchScope, stringVarFqName: String, getterFqName: String, setterFqName: String,
                                          delegateGetterFqName: String, delegateSetterFqName: String) {
        val varName = shortName(stringVarFqName)


        cacheInstance.getMethodsByName(JvmAbi.getterName(varName), scope).let {
            checkAccessorFound(it, delegateGetterFqName, stringVarFqName, true)
            checkAccessorFound(it, getterFqName, stringVarFqName, false)
            assertSize(2, it)
        }
        cacheInstance.getMethodsByName(JvmAbi.setterName(varName), scope).let {
            checkAccessorFound(it, delegateSetterFqName, stringVarFqName, true)
            checkAccessorFound(it, setterFqName, stringVarFqName, false)
            assertSize(2, it)
        }
    }

    fun checkIsVarAccessorsFoundCompanion(scope: GlobalSearchScope, stringVarFqName: String) {
        val (getter, setter) = accessorsFqNameStringFor(stringVarFqName)
        val varFqName = FqName(stringVarFqName)
        val varName = varFqName.shortName().asString()
        val companionParent = varFqName.parent().parent().asString()

        checkIsVarAccessorsFoundCompanion(scope, stringVarFqName, getter, setter,
                                          companionParent + "." + JvmAbi.getterName(varName),
                                          companionParent + "." + JvmAbi.setterName(varName))
    }

    fun accessorsFqNameStringFor(stringVarFqName: String): Pair<String, String> {
        val varFqName = FqName(stringVarFqName)
        val varShortName = varFqName.shortName().asString()
        val stringVarParentFqName = varFqName.parent().asString()
        return Pair(stringVarParentFqName + "." + JvmAbi.getterName(varShortName),
                    stringVarParentFqName + "." + JvmAbi.setterName(varShortName))
    }

    fun checkAccessorFound(methods: Array<PsiMethod>, stringFqName: String, propertyFqName: String, static: Boolean) {
        assertNotNull("Accessor $stringFqName property=$propertyFqName static=$static not found\n" + accessorArrayDebugToString(methods),
                      methods.find {
                          stringFqName == (it as KtLightMethod).clsDelegate.getKotlinFqName().toString()
                          &&
                          (it.lightMethodOrigin?.originalElement as KtProperty).fqName?.asString() == propertyFqName
                          &&
                          it.hasModifierProperty(PsiModifier.STATIC) == static
                      })
    }

    fun testGetMethodsByNameWithFunctions() {
        val file = KotlinTestUtils.navigationMetadata("idea/testData/cache/kotlinShortNamesCacheTestDataMethods.kt")
        configureByFile(file)
        val scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(myModule)
        checkIsSingleMethodFound(scope, "KotlinShortNamesCacheTestDataMethodsKt.topLevelFunction", true)
        checkIsSingleMethodFound(scope, "B1.staticMethodOfObject", true)
        checkIsSingleMethodFound(scope, "B1.nonStaticMethodOfObject", false)
        checkIsSingleMethodFound(scope, "C1.methodOfClass", false)
        checkIsSingleMethodFoundCompanion(scope, "C1.staticMethodOfCompanion", "C1.Companion.staticMethodOfCompanion")
        checkIsSingleMethodFound(scope, "C1.Companion.nonStaticMethodOfCompanion", false)
    }

    fun doTestGetMethodsByNameWithAccessors(file: String) {

        configureByFile(file)
        val scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(myModule)
        checkIsVarAccessorsFound(scope, "topLevelVar", "KotlinShortNameCacheTestData.getTopLevelVar",
                                 "KotlinShortNameCacheTestData.setTopLevelVar", true)

        checkIsVarAccessorsFound(scope, "B1.staticObjectVar", true)
        checkIsVarAccessorsFound(scope, "B1.nonStaticObjectVar", false)
        checkIsVarAccessorsFound(scope, "C1.classVar", false)
        checkIsVarAccessorsFoundCompanion(scope, "C1.Companion.staticCompanionVar")
        checkIsVarAccessorsFound(scope, "C1.Companion.nonStaticCompanionVar", false)
    }

    fun testGetMethodsByNameWithDefaultPropertyAccessors() {
        val file = KotlinTestUtils.navigationMetadata("idea/testData/cache/kotlinShortNamesCacheTestDataDefaultProperties.kt")
        doTestGetMethodsByNameWithAccessors(file)
    }

    fun testGetMethodsByNameWithCustomPropertyAccessors() {
        val file = KotlinTestUtils.navigationMetadata("idea/testData/cache/kotlinShortNamesCacheTestDataCustomProperties.kt")
        doTestGetMethodsByNameWithAccessors(file)
    }

    fun checkFieldFound(methods: Array<PsiField>, stringFqName: String, static: Boolean) {
        assertNotNull("Field $stringFqName with static=$static not found\n" + fieldArrayDebugToString(methods),
                      methods.find {
                          stringFqName == (it as KtLightField).clsDelegate.getKotlinFqName().toString()
                          &&
                          it.hasModifierProperty(PsiModifier.STATIC) == static
                      })
    }

    fun fieldArrayDebugToString(a: Array<PsiField>)
            = a.map { "${(it as KtLightField).clsDelegate.getKotlinFqName()} property=${(it.kotlinOrigin as KtProperty).fqName} static=${it.hasModifierProperty(PsiModifier.STATIC)}" }.joinToString("\n")


    fun checkIsSingleFieldFound(scope: GlobalSearchScope, stringFqName: String, static: Boolean, query: String = shortName(stringFqName)) {
        cacheInstance.getFieldsByName(query, scope).let {
            checkFieldFound(it, stringFqName, static)
            assertSize(1, it)
        }
    }

    fun testGetFieldsByName() {
        val file = KotlinTestUtils.navigationMetadata("idea/testData/cache/kotlinShortNamesCacheTestDataFields.kt")
        configureByFile(file)
        val scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(myModule)
        checkIsSingleFieldFound(scope, "KotlinShortNamesCacheTestDataFieldsKt.topLevelVar", true)
        checkIsSingleFieldFound(scope, "B1.objectVar", true)
        checkIsSingleFieldFound(scope, "C1.classVar", false)
        checkIsSingleFieldFound(scope, "C1.companionVar", true)
    }


    override fun getTestProjectJdk(): Sdk? {
        return PluginTestCaseBase.mockJdk()
    }

    override fun getTestDataPath(): String {
        return KotlinTestUtils.getHomeDirectory() + "/"
    }

}