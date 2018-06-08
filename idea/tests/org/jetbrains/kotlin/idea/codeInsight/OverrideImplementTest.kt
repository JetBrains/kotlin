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

package org.jetbrains.kotlin.idea.codeInsight

import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.idea.test.configureLanguageAndApiVersion

class OverrideImplementTest : AbstractOverrideImplementTest() {
    override fun setUp() {
        super.setUp()
        myFixture.testDataPath = PluginTestCaseBase.getTestDataPathBase() + "/codeInsight/overrideImplement"
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
        configureLanguageAndApiVersion(project, module, "1.3", "1.3")
        doOverrideFileTest("targetFun")
    }
}
