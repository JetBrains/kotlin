/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight

import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.idea.test.withCustomLanguageAndApiVersion
import org.jetbrains.kotlin.test.JUnit3WithIdeaConfigurationRunner
import org.junit.runner.RunWith

@RunWith(JUnit3WithIdeaConfigurationRunner::class)
class OverrideImplementTest : AbstractOverrideImplementTest() {
    override fun setUp() {
        super.setUp()
        myFixture.testDataPath = PluginTestCaseBase.getTestDataPathBase() + "/codeInsight/overrideImplement"
    }

    fun testAndroidxNotNull() {
        doOverrideDirectoryTest("foo")
    }

    fun testEmptyClassBodyFunctionMethod() {
        doImplementFileTest()
    }

    fun testFunctionMethod() {
        doImplementFileTest()
    }

    fun testFunctionProperty() {
        doImplementFileTest()
    }

    fun testFunctionWithTypeParameters() {
        doImplementFileTest()
    }

    fun testGenericTypesSeveralMethods() {
        doImplementFileTest()
    }

    fun testJavaInterfaceMethod() {
        doImplementDirectoryTest()
    }

    fun testJavaInterfaceMethodInCorrectOrder() {
        doMultiImplementDirectoryTest()
    }

    fun testJavaParameters() {
        doImplementDirectoryTest()
    }

    fun testFunctionFromTraitInJava() {
        doImplementJavaDirectoryTest("foo.KotlinTrait", "bar")
    }

    fun testGenericMethod() {
        doImplementFileTest()
    }

    fun testImplementJavaRawSubclass() {
        doImplementDirectoryTest()
    }

    fun testProperty() {
        doImplementFileTest()
    }

    fun testTraitGenericImplement() {
        doImplementFileTest()
    }

    fun testDefaultValues() {
        doImplementFileTest()
    }

    fun testRespectCaretPosition() {
        doMultiImplementFileTest()
    }

    fun testGenerateMulti() {
        doMultiImplementFileTest()
    }

    fun testTraitNullableFunction() {
        doImplementFileTest()
    }

    fun testOverrideUnitFunction() {
        doOverrideFileTest()
    }

    fun testOverrideNonUnitFunction() {
        doOverrideFileTest()
    }

    fun testOverrideFunctionProperty() {
        doOverrideFileTest()
    }

    fun testOverridePrimitiveProperty() {
        doMultiImplementFileTest()
    }

    fun testOverrideGenericFunction() {
        doOverrideFileTest()
    }

    fun testMultiOverride() {
        doMultiOverrideFileTest()
    }

    fun testDelegatedMembers() {
        doMultiOverrideFileTest()
    }

    fun testOverrideExplicitFunction() {
        doOverrideFileTest()
    }

    fun testOverrideExtensionFunction() {
        doOverrideFileTest()
    }

    fun testOverrideExtensionProperty() {
        doOverrideFileTest()
    }

    fun testOverrideMutableExtensionProperty() {
        doOverrideFileTest()
    }

    fun testComplexMultiOverride() {
        doMultiOverrideFileTest()
    }

    fun testOverrideRespectCaretPosition() {
        doMultiOverrideFileTest()
    }

    fun testOverrideJavaMethod() {
        doOverrideDirectoryTest("getAnswer")
    }

    fun testJavaMethodWithPackageVisibility() {
        doOverrideDirectoryTest("getFooBar")
    }

    fun testJavaMethodWithPackageProtectedVisibility() {
        doOverrideDirectoryTest("getFooBar")
    }

    fun testPrivateJavaMethod() {
        doMultiOverrideDirectoryTest()
    }

    fun testImplementSamAdapters() {
        doImplementDirectoryTest()
    }

    fun testOverrideFromFunctionPosition() {
        doMultiOverrideFileTest()
    }

    fun testOverrideFromClassName() {
        doMultiOverrideFileTest()
    }

    fun testOverrideFromLBrace() {
        doMultiOverrideFileTest()
    }

    fun testOverrideSamAdapters() {
        doOverrideDirectoryTest("foo")
    }

    fun testSameTypeName() {
        doOverrideDirectoryTest()
    }

    fun testPropagationKJK() {
        doOverrideDirectoryTest()
    }

    fun testMultipleSupers() {
        doMultiOverrideFileTest()
    }

    fun testNoAnyMembersInInterface() {
        doMultiOverrideFileTest()
    }

    fun testLocalClass() {
        doImplementFileTest()
    }

    fun testStarProjections() {
        doImplementFileTest()
    }

    fun testEscapeIdentifiers() {
        doOverrideFileTest()
    }

    fun testVarArgs() {
        doOverrideFileTest()
    }

    fun testSuspendFun() {
        doOverrideFileTest()
    }

    fun testDoNotOverrideFinal() {
        doMultiOverrideFileTest()
    }

    fun testSuperPreference() {
        doMultiOverrideFileTest()
    }

    fun testAmbiguousSuper() {
        doMultiOverrideFileTest()
    }

    fun testImplementFunctionType() {
        doMultiImplementFileTest()
    }

    fun testQualifySuperType() {
        doOverrideFileTest("f")
    }

    fun testGenericSuperClass() {
        doOverrideFileTest("iterator")
    }

    fun testDuplicatedAnyMembersBug() {
        doMultiOverrideFileTest()
    }

    fun testEqualsInInterface() {
        doOverrideFileTest("equals")
    }

    fun testCopyKDoc() {
        doOverrideFileTest("foo")
    }

    fun testConvertJavaDoc() {
        doOverrideDirectoryTest("foo")
    }

    fun testPlatformTypes() {
        doOverrideDirectoryTest("foo")
    }

    fun testPlatformCollectionTypes() {
        doOverrideDirectoryTest("foo")
    }

    fun testNullableJavaType() {
        doOverrideDirectoryTest("foo")
    }

    fun testJavaxNonnullJavaType() {
        doOverrideDirectoryTest("foo")
    }

    fun testNullableKotlinType() {
        doOverrideDirectoryTest("foo")
    }

    fun testAbstractAndNonAbstractInheritedFromInterface() {
        doImplementFileTest("getFoo")
    }

    fun testTypeAliasNotExpanded() {
        doOverrideFileTest("test")
    }

    fun testDataClassEquals() {
        doOverrideFileTest("equals")
    }

    fun testCopyExperimental() {
        withCustomLanguageAndApiVersion(project, module, "1.3", "1.3") {
            doOverrideFileTest("targetFun")
        }
    }

    fun testUnresolvedType() {
        doOverrideFileTest()
    }

    fun testImplementFromClassName() {
        doMultiImplementFileTest()
    }

    fun testImplementFromClassName2() {
        doMultiImplementFileTest()
    }

    fun testImplementFromClassName3() {
        doMultiImplementFileTest()
    }

    fun testImplementFromClassName4() {
        doMultiImplementFileTest()
    }

    fun testImplementFromClassName5() {
        doMultiImplementFileTest()
    }

    fun testImplementFromClassName6() {
        doMultiImplementFileTest()
    }

    fun testEnumClass() {
        doOverrideFileTest("toString")
    }

    fun testEnumClass2() {
        doOverrideFileTest("toString")
    }

    fun testEnumClass3() {
        doOverrideFileTest("toString")
    }

    fun testEnumClass4() {
        doOverrideFileTest("toString")
    }
}
