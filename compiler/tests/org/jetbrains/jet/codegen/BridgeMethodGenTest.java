/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.codegen;

import org.jetbrains.jet.ConfigurationKind;

public class BridgeMethodGenTest extends CodegenTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
    }

    public void testOverrideReturnType() {
        blackBoxFile("bridges/overrideReturnType.kt");
    }

    public void testKt1959() {
        blackBoxFile("regressions/kt1959.kt");
    }

    public void testDelegation() {
        blackBoxFile("bridges/delegation.kt");
    }

    public void testDelegationProperty() {
        blackBoxFile("bridges/delegationProperty.kt");
    }

    public void testDelegationToTraitImpl() {
        blackBoxFile("bridges/delegationToTraitImpl.kt");
    }

    public void testDiamond() {
        blackBoxFile("bridges/diamond.kt");
    }

    public void testLongChainOneBridge() {
        blackBoxFile("bridges/longChainOneBridge.kt");
    }

    public void testManyTypeArgumentsSubstitutedSuccessively() {
        blackBoxFile("bridges/manyTypeArgumentsSubstitutedSuccessively.kt");
    }

    public void testMethodFromTrait() {
        blackBoxFile("bridges/methodFromTrait.kt");
    }

    public void testOverrideAbstractProperty() {
        blackBoxFile("bridges/overrideAbstractProperty.kt");
    }

    public void testSimple() {
        blackBoxFile("bridges/simple.kt");
    }

    public void testSimpleEnum() {
        blackBoxFile("bridges/simpleEnum.kt");
    }

    public void testSimpleGenericMethod() {
        blackBoxFile("bridges/simpleGenericMethod.kt");
    }

    public void testSimpleObject() {
        blackBoxFile("bridges/simpleObject.kt");
    }

    public void testSimpleReturnType() {
        blackBoxFile("bridges/simpleReturnType.kt");
    }

    public void testSimpleUpperBound() {
        blackBoxFile("bridges/simpleUpperBound.kt");
    }

    public void testSubstitutionInSuperClass() {
        blackBoxFile("bridges/substitutionInSuperClass.kt");
    }

    public void testSubstitutionInSuperClassDelegation() {
        blackBoxFile("bridges/substitutionInSuperClassDelegation.kt");
    }

    public void testSubstitutionInSuperClassAbstractFun() {
        blackBoxFile("bridges/substitutionInSuperClassAbstractFun.kt");
    }

    public void testSubstitutionInSuperClassBoundedTypeArguments() {
        blackBoxFile("bridges/substitutionInSuperClassBoundedTypeArguments.kt");
    }

    public void testSubstitutionInSuperClassEnum() {
        blackBoxFile("bridges/substitutionInSuperClassEnum.kt");
    }

    public void testSubstitutionInSuperClassGenericMethod() {
        blackBoxFile("bridges/substitutionInSuperClassGenericMethod.kt");
    }

    public void testSubstitutionInSuperClassObject() {
        blackBoxFile("bridges/substitutionInSuperClassObject.kt");
    }

    public void testSubstitutionInSuperClassUpperBound() {
        blackBoxFile("bridges/substitutionInSuperClassUpperBound.kt");
    }

    public void testTwoParentsWithDifferentMethodsTwoBridges() {
        blackBoxFile("bridges/twoParentsWithDifferentMethodsTwoBridges.kt");
    }

    public void testTwoParentsWithTheSameMethodOneBridge() {
        blackBoxFile("bridges/twoParentsWithTheSameMethodOneBridge.kt");
    }

    public void testKt2498() {
        blackBoxFile("regressions/kt2498.kt");
    }

    public void testKt1939() {
        blackBoxFile("regressions/kt1939.kt");
    }

    public void testKt2702() {
        blackBoxFile("regressions/kt2702.kt");
    }

    public void testKt2920() {
        blackBoxFile("regressions/kt2920.kt");
    }

    public void testSubstitutionInSuperClassProperty() {
        blackBoxFile("bridges/substitutionInSuperClassProperty.kt");
    }

    public void testPropertySetter() {
        blackBoxFile("bridges/propertySetter.kt");
    }

    public void testPropertyDiamond() {
        blackBoxFile("bridges/propertyDiamond.kt");
    }

    public void testPropertyAccessorsWithoutBody() {
        blackBoxFile("bridges/propertyAccessorsWithoutBody.kt");
    }

    public void testPropertyInConstructor() {
        blackBoxFile("bridges/propertyInConstructor.kt");
    }

    public void testKt2833() {
        blackBoxFile("regressions/kt2833.kt");
    }
}
