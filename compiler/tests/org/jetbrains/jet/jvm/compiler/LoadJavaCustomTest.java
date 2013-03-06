/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.jvm.compiler;

import org.jetbrains.annotations.NotNull;

/*
   LoadJavaTestGenerated should be used instead if possible.
 */
public final class LoadJavaCustomTest extends AbstractLoadJavaTest {
    @NotNull
    private static final String PATH = "compiler/testData/loadJavaCustom";

    public void testPackageLocalVisibility() throws Exception {
        String dir = PATH + "/packageLocalVisibility/simple/";
        String javaDir = dir + "/java";
        doTestCompiledJava(dir + "/expected.txt",
                           javaDir + "/test/JFrame.java",
                           javaDir + "/awt/Frame.java");
    }

    public void testInner() throws Exception {
        doSimpleTest();
    }

    public void testProtectedStaticVisibility() throws Exception {
        String dir = PATH + "/protectedStaticVisibility/constructor/";
        doTestCompiledJava(dir + "ConstructorInProtectedStaticNestedClass.txt",
                           dir + "ConstructorInProtectedStaticNestedClass.java");
    }

    public void testProtectedPackageFun() throws Exception {
        String dir = PATH + "/protectedPackageVisibility/";
        doTestCompiledJava(dir + "ProtectedPackageFun.txt",
                           dir + "ProtectedPackageFun.java");
    }

    public void testProtectedPackageConstructor() throws Exception {
        String dir = PATH + "/protectedPackageVisibility/";
        doTestCompiledJava(dir + "ProtectedPackageConstructor.txt",
                           dir + "ProtectedPackageConstructor.java");
    }

    public void testProtectedPackageProperty() throws Exception {
        String dir = PATH + "/protectedPackageVisibility/";
        doTestCompiledJava(dir + "ProtectedPackageProperty.txt",
                           dir + "ProtectedPackageProperty.java");
    }

    public void testStaticFinal() throws Exception {
        String dir = "/staticFinal/";
        doTestCompiledJava(PATH + dir + "expected.txt",
               PATH + dir + "test.java");
    }

    private void doSimpleTest() throws Exception {
        doTestCompiledJava(PATH + "/" + getTestName(true) + ".txt",
               PATH + "/" + getTestName(true) + ".java");
    }

    public void testKotlinSignatureTwoSuperclassesInconsistentGenericTypes() throws Exception {
        String dir = PATH + "/kotlinSignature/";
        doTestCompiledJava(dir + "TwoSuperclassesInconsistentGenericTypes.txt",
                           dir + "TwoSuperclassesInconsistentGenericTypes.java");
    }

    public void testKotlinSignatureTwoSuperclassesVarargAndNot() throws Exception {
        String dir = PATH + "/kotlinSignature/";
        doTestCompiledJava(dir + "TwoSuperclassesVarargAndNot.txt",
                           dir + "TwoSuperclassesVarargAndNot.java");
    }

    //TODO: move to LoadJavaTestGenerated when possible
    public void testEnum() throws Exception {
        String dir = PATH + "/enum";
        String javaDir = dir + "/java";
        doTestCompiledJava(dir + "/expected.txt",
                           javaDir + "/MyEnum.java");
    }

    public void testRawSuperType() throws Exception {
        String dir = PATH + "/rawSuperType/";
        doTestCompiledJava(dir + "RawSuperType.txt",
                           dir + "RawSuperType.java");
    }

    public void testSubclassWithRawType() throws Exception {
        String dir = PATH + "/subclassWithRawType/";
        doTestCompiledJava(dir + "SubclassWithRawType.txt",
                           dir + "SubclassWithRawType.java");
    }

    public void testArraysInSubtypes() throws Exception {
        String dir = PATH + "/arraysInSubtypes/";
        doTestCompiledJava(dir + "ArraysInSubtypes.txt",
                           dir + "ArraysInSubtypes.java");
    }

    public void testMethodTypeParameterErased() throws Exception {
        String dir = PATH + "/methodTypeParameterErased/";
        doTestCompiledJava(dir + "MethodTypeParameterErased.txt",
                           dir + "MethodTypeParameterErased.java");
    }

    public void testReturnInnerSubclassOfSupersInner() throws Exception {
        String dir = PATH + "/returnInnerSubclassOfSupersInner/";
        doTestCompiledJava(dir + "ReturnInnerSubclassOfSupersInner.txt",
                           dir + "test/ReturnInnerSubclassOfSupersInner.java");
    }

    public void testReturnInnerSubclassOfSupersInnerNoCompile() throws Exception {
        // Test is here because Java PSI used to have some differences when loading parallel generic hierarchies from cls and source code.
        String dir = PATH + "/returnInnerSubclassOfSupersInner/";
        doTestSourceJava(dir + "ReturnInnerSubclassOfSupersInner.txt", dir);
    }

    public void testReturnNotSubtype() throws Exception {
        String dir = PATH + "/returnNotSubtype/";
        doTestSourceJava(dir + "ReturnNotSubtype.txt", dir);
    }

    public void testErrorTypes() throws Exception {
        String dir = PATH + "/errorTypes/";
        doTestSourceJava(dir + "ErrorTypes.txt", dir);
    }

    public static class SubclassingKotlinInJavaTest extends AbstractLoadJavaTest {
        public void testSubclassingKotlinInJava() throws Exception {
            doTestJavaAgainstKotlin(PATH + "/" + getTestName(true));
        }

        public void testDeepSubclassingKotlinInJava() throws Exception {
            doTestJavaAgainstKotlin(PATH + "/" + getTestName(true));
        }

        public void testPackageInheritance() throws Exception {
            doTestJavaAgainstKotlin(PATH + "/packageLocalVisibility/inheritance");
        }

        public void testProtectedPackageInheritance() throws Exception {
            doTestJavaAgainstKotlin(PATH + "/protectedPackageVisibility/inheritance");
        }
    }
}
