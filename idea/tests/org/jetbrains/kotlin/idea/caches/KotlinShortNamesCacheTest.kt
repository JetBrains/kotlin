/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches

import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.testFramework.LightProjectDescriptor
import com.sun.tools.javac.util.Convert.shortName
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.test.KotlinTestUtils
import kotlin.reflect.KMutableProperty0

class KotlinShortNamesCacheTest : KotlinLightCodeInsightFixtureTestCase() {

    private lateinit var cacheInstance: PsiShortNamesCache

    override fun getProjectDescriptor(): LightProjectDescriptor = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    override fun setUp() {
        super.setUp()
        cacheInstance = KotlinShortNamesCache(project)
    }

    override fun tearDown() {
        (this::cacheInstance as KMutableProperty0<GlobalSearchScope?>).set(null)
        super.tearDown()
    }

    fun testGetMethodsByNameIfNotMoreThanLimits() {
        myFixture.configureByFile("kotlinShortNamesCacheTestData1.kt")
        val scope = GlobalSearchScope.allScope(project)
        assertSize(2, cacheInstance.getMethodsByNameIfNotMoreThan("foobar", scope, 2))
        assertSize(3, cacheInstance.getMethodsByNameIfNotMoreThan("foobar", scope, 3))
        assertSize(3, cacheInstance.getMethodsByNameIfNotMoreThan("foobar", scope, Int.MAX_VALUE))
    }

    fun testGetFieldsByNameIfNotMoreThanLimits() {
        myFixture.configureByFile("kotlinShortNamesCacheTestData1.kt")
        val scope = GlobalSearchScope.allScope(project)
        assertSize(2, cacheInstance.getFieldsByNameIfNotMoreThan("barfoo", scope, 2))
        assertSize(3, cacheInstance.getFieldsByNameIfNotMoreThan("barfoo", scope, 3))
        assertSize(3, cacheInstance.getFieldsByNameIfNotMoreThan("barfoo", scope, Int.MAX_VALUE))
    }

    fun testGetAllFields() {
        myFixture.configureByFile("kotlinShortNamesCacheTestData2.kt")
        val allFieldNameList = cacheInstance.allFieldNames.asList()
        assertContainsElements(allFieldNameList, "foobar")
        assertContainsElements(allFieldNameList, "barfoo")
    }

    fun testGetAllMethods() {
        myFixture.configureByFile("kotlinShortNamesCacheTestData2.kt")
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
        myFixture.configureByFile("kotlinShortNamesCacheTestDataClasses.kt")
        val allClassNameList = cacheInstance.allClassNames.asList()
        assertContainsElements(allClassNameList, "C1")
        assertContainsElements(allClassNameList, "O1")
    }

    fun methodArrayDebugToString(a: Array<PsiMethod>)
            = a.map { "${(it as KtLightMethod).clsDelegate.getKotlinFqName()} static=${it.hasModifierProperty(PsiModifier.STATIC)}" }.joinToString("\n")

    fun accessorArrayDebugToString(a: Array<PsiMethod>)
            = a.map { "${(it as KtLightMethod).clsDelegate.getKotlinFqName()} property=${(it.lightMemberOrigin?.originalElement as KtProperty).fqName} static=${it.hasModifierProperty(PsiModifier.STATIC)}" }.joinToString("\n")

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
                          (it.lightMemberOrigin?.originalElement as KtProperty).fqName?.asString() == propertyFqName
                          &&
                          it.hasModifierProperty(PsiModifier.STATIC) == static
                      })
    }

    fun testGetMethodsByNameWithFunctions() {
        myFixture.configureByFile("kotlinShortNamesCacheTestDataMethods.kt")
        val scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(myModule)
        checkIsSingleMethodFound(scope, "KotlinShortNamesCacheTestDataMethodsKt.topLevelFunction", true)
        checkIsSingleMethodFound(scope, "B1.staticMethodOfObject", true)
        checkIsSingleMethodFound(scope, "B1.nonStaticMethodOfObject", false)
        checkIsSingleMethodFound(scope, "C1.methodOfClass", false)
        checkIsSingleMethodFoundCompanion(scope, "C1.staticMethodOfCompanion", "C1.Companion.staticMethodOfCompanion")
        checkIsSingleMethodFound(scope, "C1.Companion.nonStaticMethodOfCompanion", false)
    }

    fun doTestGetMethodsByNameWithAccessors(file: String) {
        myFixture.configureByFile(file)
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
        doTestGetMethodsByNameWithAccessors("kotlinShortNamesCacheTestDataDefaultProperties.kt")
    }

    fun testGetMethodsByNameWithCustomPropertyAccessors() {
        doTestGetMethodsByNameWithAccessors("kotlinShortNamesCacheTestDataCustomProperties.kt")
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
        myFixture.configureByFile("kotlinShortNamesCacheTestDataFields.kt")
        val scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(myModule)
        checkIsSingleFieldFound(scope, "KotlinShortNamesCacheTestDataFieldsKt.topLevelVar", true)
        checkIsSingleFieldFound(scope, "B1.objectVar", true)
        checkIsSingleFieldFound(scope, "C1.classVar", false)
        checkIsSingleFieldFound(scope, "C1.companionVar", true)
    }

    override fun getTestDataPath(): String {
        return KotlinTestUtils.getHomeDirectory() + "/idea/testData/cache/"
    }
}