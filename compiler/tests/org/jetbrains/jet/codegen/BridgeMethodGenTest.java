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

    public void testBridgeMethod () throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("bridge.jet");
    }

    public void testKt1959() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt1959.kt");
    }

    public void testDelegationToTraitImpl() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("bridges/delegationToTraitImpl.kt");
    }

    public void testDiamond() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("bridges/diamond.kt");
    }

    public void testLongChainOneBridge() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("bridges/longChainOneBridge.kt");
    }

    public void testManyTypeArgumentsSubstitutedSuccessively() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("bridges/manyTypeArgumentsSubstitutedSuccessively.kt");
    }

    public void testMethodFromTrait() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("bridges/methodFromTrait.kt");
    }

    public void testSimple() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("bridges/simple.kt");
    }

    public void testSimpleEnum() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("bridges/simpleEnum.kt");
    }

    public void testSimpleGenericMethod() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("bridges/simpleGenericMethod.kt");
    }

    public void testSimpleObject() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("bridges/simpleObject.kt");
    }

    public void testSimpleReturnType() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("bridges/simpleReturnType.kt");
    }

    public void testSimpleUpperBound() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("bridges/simpleUpperBound.kt");
    }

    public void testSubstitutionInSuperClass() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("bridges/substitutionInSuperClass.kt");
    }

    public void testSubstitutionInSuperClassAbstractFun() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("bridges/substitutionInSuperClassAbstractFun.kt");
    }

    public void testSubstitutionInSuperClassBoundedTypeArguments() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("bridges/substitutionInSuperClassBoundedTypeArguments.kt");
    }

    public void testSubstitutionInSuperClassEnum() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("bridges/substitutionInSuperClassEnum.kt");
    }

    public void testSubstitutionInSuperClassGenericMethod() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("bridges/substitutionInSuperClassGenericMethod.kt");
    }

    public void testSubstitutionInSuperClassObject() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("bridges/substitutionInSuperClassObject.kt");
    }

    public void testSubstitutionInSuperClassUpperBound() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("bridges/substitutionInSuperClassUpperBound.kt");
    }

    public void testTwoParentsWithDifferentMethodsTwoBridges() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("bridges/twoParentsWithDifferentMethodsTwoBridges.kt");
    }

    public void testTwoParentsWithTheSameMethodOneBridge() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("bridges/twoParentsWithTheSameMethodOneBridge.kt");
    }

    public void testKt2498() {
        createEnvironmentWithFullJdk();
        blackBoxFile("regressions/kt2498.kt");
    }
}
