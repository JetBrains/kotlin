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
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

public class ClassGenTest extends CodegenTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testPSVMClass() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        loadFile("classes/simpleClass.jet");

        final Class aClass = loadClass("SimpleClass", generateClassesInFile());
        final Method[] methods = aClass.getDeclaredMethods();
        // public int SimpleClass.foo()
        // public jet.TypeInfo SimpleClass.getTypeInfo()
        assertEquals(1, methods.length);
    }

    public void testArrayListInheritance() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_AND_ANNOTATIONS);
        loadFile("classes/inheritingFromArrayList.jet");
        //        System.out.println(generateToText());
        final Class aClass = loadClass("Foo", generateClassesInFile());
        assertInstanceOf(aClass.newInstance(), List.class);
    }

    public void testDelegationJavaIface() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("classes/delegationJava.kt");
    }

    public void testDelegationToVal() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        loadFile("classes/delegationToVal.kt");
        //        System.out.println(generateToText());
        final ClassFileFactory state = generateClassesInFile();
        final GeneratedClassLoader loader = createClassLoader(state);
        final Class aClass = loader.loadClass(PackageClassUtils.getPackageClassName(FqName.ROOT));
        assertEquals("OK", aClass.getMethod("box").invoke(null));

        final Class test = loader.loadClass("Test");
        try {
            test.getDeclaredField("$delegate_0");
            fail("$delegate_0 field generated for class Test but should not");
        }
        catch (NoSuchFieldException e) {}

        final Class test2 = loader.loadClass("Test2");
        try {
            test2.getDeclaredField("$delegate_0");
            fail("$delegate_0 field generated for class Test2 but should not");
        }
        catch (NoSuchFieldException e) {}

        final Class test3 = loader.loadClass("Test3");
        final Class iActing = loader.loadClass("IActing");
        final Object obj = test3.newInstance();
        assertTrue(iActing.isInstance(obj));
        final Method iActingMethod = iActing.getMethod("act");
        assertEquals("OK", iActingMethod.invoke(obj));
        assertEquals("OKOK", iActingMethod.invoke(test3.getMethod("getActing").invoke(obj)));
    }

    public void testInheritanceAndDelegation_DelegatingDefaultConstructorProperties() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("classes/inheritance.jet");
    }

    public void testInheritanceAndDelegation2() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("classes/delegation2.kt");
    }

    public void testInheritanceAndDelegation3() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("classes/delegation3.kt");
    }

    public void testInheritanceAndDelegation4() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("classes/delegation4.kt");
    }

    public void testInheritanceAndDelegationTyped() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("classes/typedDelegation.kt");
    }

    public void testDelegationMethodsWithArgs() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("classes/delegationMethodsWithArgs.kt");
    }

    public void testDelegationGenericArg() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("classes/delegationGenericArg.kt");
    }

    public void testDelegationGenericLongArg() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("classes/delegationGenericLongArg.kt");
    }

    public void testDelegationGenericArgUpperBound() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("classes/delegationGenericArgUpperBound.kt");
    }

    public void testFunDelegation() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("classes/funDelegation.jet");
    }

    public void testPropertyDelegation() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("classes/propertyDelegation.jet");
    }

    public void testDiamondInheritance() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("classes/diamondInheritance.jet");
    }

    public void testRightHandOverride() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("classes/rightHandOverride.jet");
    }

    public void testNewInstanceExplicitConstructor() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        loadFile("classes/newInstanceDefaultConstructor.jet");
        //        System.out.println(generateToText());
        final Method method = generateFunction("test");
        final Integer returnValue = (Integer) method.invoke(null);
        assertEquals(610, returnValue.intValue());
    }

    public void testInnerClass() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("classes/innerClass.jet");
    }

    public void testInheritedInnerClass() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("classes/inheritedInnerClass.jet");
    }

    public void testKt2532() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("classes/kt2532.kt");
    }

    public void testInitializerBlock() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("classes/initializerBlock.jet");
    }

    public void testAbstractMethod() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        loadText("abstract class Foo { abstract fun x(): String; fun y(): Int = 0 }");

        final ClassFileFactory codegens = generateClassesInFile();
        final Class aClass = loadClass("Foo", codegens);
        assertNotNull(aClass.getMethod("x"));
        assertNotNull(findMethodByName(aClass, "y"));
    }

    public void testInheritedMethod() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("classes/inheritedMethod.jet");
    }

    public void testInitializerBlockDImpl() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("classes/initializerBlockDImpl.jet");
    }

    public void testPropertyInInitializer() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("classes/propertyInInitializer.jet");
    }

    public void testOuterThis() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("classes/outerThis.jet");
    }

    public void testExceptionConstructor() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("classes/exceptionConstructor.jet");
    }

    public void testSimpleBox() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("classes/simpleBox.jet");
    }

    public void testAbstractClass() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        loadText("abstract class SimpleClass() { }");

        final Class aClass = createClassLoader(generateClassesInFile()).loadClass("SimpleClass");
        assertTrue((aClass.getModifiers() & Modifier.ABSTRACT) != 0);
    }

    public void testClassObject() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("classes/classObject.jet");
    }

    public void testClassObjectInTrait() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("classes/classObjectInTrait.jet");
    }

    public void testClassObjectMethod() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        // todo to be implemented after removal of type info
        //        blackBoxFile("classes/classObjectMethod.jet");
    }

    public void testClassObjectInterface() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        loadFile("classes/classObjectInterface.jet");
        final Method method = generateFunction();
        Object result = method.invoke(null);
        assertInstanceOf(result, Runnable.class);
    }

    public void testOverloadBinaryOperator() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("classes/overloadBinaryOperator.jet");
    }

    public void testOverloadUnaryOperator() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("classes/overloadUnaryOperator.jet");
    }

    public void testOverloadPlusAssign() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("classes/overloadPlusAssign.jet");
    }

    public void testOverloadPlusAssignReturn() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("classes/overloadPlusAssignReturn.jet");
    }

    public void testOverloadPlusToPlusAssign() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("classes/overloadPlusToPlusAssign.jet");
    }

    public void testEnumClass() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        loadText("enum class Direction { NORTH; SOUTH; EAST; WEST }");
        final Class direction = createClassLoader(generateClassesInFile()).loadClass("Direction");
        //        System.out.println(generateToText());
        final Field north = direction.getField("NORTH");
        assertEquals(direction, north.getType());
        assertInstanceOf(north.get(null), direction);
    }

    public void testEnumConstantConstructors() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        loadText("enum class Color(val rgb: Int) { RED: Color(0xFF0000); GREEN: Color(0x00FF00); }");
        final Class colorClass = createClassLoader(generateClassesInFile()).loadClass("Color");
        final Field redField = colorClass.getField("RED");
        final Object redValue = redField.get(null);
        final Method rgbMethod = colorClass.getMethod("getRgb");
        assertEquals(0xFF0000, rgbMethod.invoke(redValue));
    }

    public void testClassObjFields() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        loadText("class A() { class object { val value = 10 } }\n" +
                 "fun box() = if(A.value == 10) \"OK\" else \"fail\"");
        blackBox();
    }

    public void testPrivateOuterProperty() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("classes/privateOuterProperty.kt");
    }

    public void testPrivateOuterFunctions() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("classes/privateOuterFunctions.kt");
    }

    public void testKt249() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt249.jet");
    }

    public void testKt48() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt48.jet");
        //        System.out.println(generateToText());
    }

    public void testKt309() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        loadText("fun box() = null");
        final Method method = generateFunction("box");
        assertEquals(method.getReturnType().getName(), "java.lang.Object");
        //        System.out.println(generateToText());
    }

    public void testKt343() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt343.jet");
        //        System.out.println(generateToText());
    }

    public void testKt508() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        loadFile("regressions/kt508.jet");
        //        System.out.println(generateToText());
        blackBox();
    }

    public void testKt504() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        loadFile("regressions/kt504.jet");
        //        System.out.println(generateToText());
        blackBox();
    }

    public void testKt501() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt501.jet");
    }

    public void testKt496() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt496.jet");
        //        System.out.println(generateToText());
    }

    public void testKt500() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt500.jet");
    }

    public void testKt694() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        //        blackBoxFile("regressions/kt694.jet");
    }

    public void testKt285() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        //        blackBoxFile("regressions/kt285.jet");
    }

    public void testKt707() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt707.jet");
    }

    public void testKt857() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        //        blackBoxFile("regressions/kt857.jet");
    }

    public void testKt903() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_AND_ANNOTATIONS);
        blackBoxFile("regressions/kt903.jet");
    }

    public void testKt940() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt940.kt");
    }

    public void testKt1018() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt1018.kt");
        //System.out.println(generateToText());
    }

    public void testKt1120() throws Exception {
        //createEnvironmentWithFullJdk();
        //        blackBoxFile("regressions/kt1120.kt");
    }

    public void testSelfCreate() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("classes/selfcreate.kt");
    }

    public void testKt1134() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt1134.kt");
    }

    public void testKt1157() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt1157.kt");
    }

    public void testKt471() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt471.kt");
    }

    public void testKt1213() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations();
        //        blackBoxFile("regressions/kt1213.kt");
    }

    public void testKt723() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt723.kt");
    }

    public void testKt725() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt725.kt");
    }

    public void testKt633() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt633.kt");
    }


    public void testKt1345() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt1345.kt");
    }

    public void testKt1538() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.ALL);
        blackBoxFile("regressions/kt1538.kt");
    }

    public void testKt1759() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt1759.kt");
    }

    public void testResolveOrder() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("classes/resolveOrder.jet");
    }

    public void testKt1918() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt1918.kt");
    }

    public void testKt1247() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt1247.kt");
    }

    public void testKt1980() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_AND_ANNOTATIONS);
        blackBoxFile("regressions/kt1980.kt");
    }

    public void testKt1578() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt1578.kt");
    }

    public void testKt1726() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt1726.kt");
    }

    public void testKt1721() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt1721.kt");
    }

    public void testKt1976() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt1976.kt");
    }

    public void testKt1439() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt1439.kt");
    }

    public void testKt1611() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt1611.kt");
    }

    public void testKt1891() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt1891.kt");
    }

    public void testKt2224() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt2224.kt");
    }

    public void testKt2384() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt2384.kt");
    }

    public void testKt2390() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt2390.kt");
    }

    public void testKt2391() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt2391.kt");
    }

    public void testKt2060() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxMultiFile("regressions/kt2060_1.kt", "regressions/kt2060.kt");
    }

    public void testKt2395() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_AND_ANNOTATIONS);
        blackBoxMultiFile("regressions/kt2395.kt");
    }

    public void testKt2566() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxMultiFile("regressions/kt2566.kt");
    }

    public void testKt2566_2() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxMultiFile("regressions/kt2566_2.kt");
    }

    public void testKt2477() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt2477.kt");
    }

    public void testKt2485() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxMultiFile("regressions/kt2485.kt");
    }

    public void testKt2482() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxMultiFile("regressions/kt2482.kt");
    }

    public void testKt2288() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        loadFile("regressions/kt2288.kt");
        //System.out.println(generateToText());
        blackBox();
    }

    public void testKt2257() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxMultiFile("regressions/kt2257_1.kt", "regressions/kt2257_2.kt");
    }

    public void testKt1845() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxMultiFile("regressions/kt1845_1.kt", "regressions/kt1845_2.kt");
    }

    public void testKt2417() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt2417.kt");
    }

    public void testKt2480() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt2480.kt");
    }

    public void testKt454() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt454.kt");
    }

    public void testKt1535() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt1535.kt");
    }

    public void testKt2711() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt2711.kt");
    }

    public void testKt2626() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt2626.kt");
    }

    public void testKt2781() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFileWithJava("regressions/kt2781.kt", true);
    }

    public void testKt2607() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt2607.kt");
    }
}
