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

public class OverrideImplementTest : AbstractOverrideImplementTest() {
    override fun setUp() {
        super.setUp()
        myFixture.testDataPath = PluginTestCaseBase.getTestDataPathBase() + "/codeInsight/overrideImplement"
    }

    public fun testEmptyClassBodyFunctionMethod() {
        doImplementFileTest()
    }

    public fun testFunctionMethod() {
        doImplementFileTest()
    }

    public fun testFunctionProperty() {
        doImplementFileTest()
    }

    public fun testFunctionWithTypeParameters() {
        doImplementFileTest()
    }

    public fun testGenericTypesSeveralMethods() {
        doImplementFileTest()
    }

    public fun testJavaInterfaceMethod() {
        doImplementDirectoryTest()
    }

    public fun testJavaParameters() {
        doImplementDirectoryTest()
    }

    public fun testFunctionFromTraitInJava() {
        doImplementJavaDirectoryTest("foo.KotlinTrait", "bar")
    }

    public fun testGenericMethod() {
        doImplementFileTest()
    }

    public fun testImplementJavaRawSubclass() {
        doImplementDirectoryTest()
    }

    public fun testProperty() {
        doImplementFileTest()
    }

    public fun testTraitGenericImplement() {
        doImplementFileTest()
    }

    public fun testDefaultValues() {
        doImplementFileTest()
    }

    public fun testRespectCaretPosition() {
        doMultiImplementFileTest()
    }

    public fun testGenerateMulti() {
        doMultiImplementFileTest()
    }

    public fun testTraitNullableFunction() {
        doImplementFileTest()
    }

    public fun testOverrideUnitFunction() {
        doOverrideFileTest()
    }

    public fun testOverrideNonUnitFunction() {
        doOverrideFileTest()
    }

    public fun testOverrideFunctionProperty() {
        doOverrideFileTest()
    }

    public fun testOverridePrimitiveProperty() {
        doMultiImplementFileTest()
    }

    public fun testOverrideGenericFunction() {
        doOverrideFileTest()
    }

    public fun testMultiOverride() {
        doMultiOverrideFileTest()
    }

    public fun testDelegatedMembers() {
        doMultiOverrideFileTest()
    }

    public fun testOverrideExplicitFunction() {
        doOverrideFileTest()
    }

    public fun testOverrideExtensionProperty() {
        doOverrideFileTest()
    }

    public fun testOverrideMutableExtensionProperty() {
        doOverrideFileTest()
    }

    public fun testComplexMultiOverride() {
        doMultiOverrideFileTest()
    }

    public fun testOverrideRespectCaretPosition() {
        doMultiOverrideFileTest()
    }

    public fun testOverrideJavaMethod() {
        doOverrideDirectoryTest("getAnswer")
    }

    public fun testJavaMethodWithPackageVisibility() {
        doOverrideDirectoryTest("getFooBar")
    }

    public fun testJavaMethodWithPackageProtectedVisibility() {
        doOverrideDirectoryTest("getFooBar")
    }

    public fun testPrivateJavaMethod() {
        doMultiOverrideDirectoryTest()
    }

    public fun testInheritVisibilities() {
        doMultiOverrideFileTest()
    }

    public fun testImplementSamAdapters() {
        doImplementDirectoryTest()
    }

    public fun testOverrideFromFunctionPosition() {
        doMultiOverrideFileTest()
    }

    public fun testOverrideFromClassName() {
        doMultiOverrideFileTest()
    }

    public fun testOverrideFromLBrace() {
        doMultiOverrideFileTest()
    }

    public fun testOverrideSamAdapters() {
        doOverrideDirectoryTest("foo")
    }

    public fun testSameTypeName() {
        doOverrideDirectoryTest()
    }

    public fun testPropagationKJK() {
        doOverrideDirectoryTest()
    }

    public fun testMultipleSupers() {
        doMultiOverrideFileTest()
    }

    public fun testLocalClass() {
        doImplementFileTest()
    }

    public fun testStarProjections() {
        doImplementFileTest()
    }

    public fun testEscapeIdentifiers() {
        doOverrideFileTest()
    }

    public fun testVarArgs() {
        doOverrideFileTest()
    }

    public fun testDoNotOverrideFinal() {
        doMultiOverrideFileTest()
    }

    public fun testSuperPreference() {
        doMultiOverrideFileTest()
    }

    public fun testAmbiguousSuper() {
        doMultiOverrideFileTest()
    }

    public fun testImplementFunctionType() {
        doMultiImplementFileTest()
    }

    public fun testQualifySuperType() {
        doOverrideFileTest("f")
    }

    public fun testGenericSuperClass() {
        doOverrideFileTest("iterator")
    }

    public fun testDuplicatedAnyMembersBug() {
        doMultiOverrideFileTest()
    }
}
