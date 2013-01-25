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

import static org.jetbrains.jet.codegen.CodegenTestUtil.findDeclaredMethodByName;

public class ClassGenTest extends CodegenTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
    }

    public void testPSVMClass() {
        loadFile("classes/simpleClass.kt");

        final Class aClass = generateClass("SimpleClass");
        final Method[] methods = aClass.getDeclaredMethods();
        // public int SimpleClass.foo()
        assertEquals(1, methods.length);
    }

    public void testArrayListInheritance() throws Exception {
        loadFile("classes/inheritingFromArrayList.kt");
        final Class aClass = generateClass("Foo");
        assertInstanceOf(aClass.newInstance(), List.class);
    }

    public void testDelegationJavaIface() {
        blackBoxFile("classes/delegationJava.kt");
    }

    public void testDelegationToVal() throws Exception {
        loadFile("classes/delegationToVal.kt");
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

    public void testInheritanceAndDelegation_DelegatingDefaultConstructorProperties() {
        blackBoxFile("classes/inheritance.kt");
    }

    public void testInheritanceAndDelegation2() {
        blackBoxFile("classes/delegation2.kt");
    }

    public void testInheritanceAndDelegation3() {
        blackBoxFile("classes/delegation3.kt");
    }

    public void testInheritanceAndDelegation4() {
        blackBoxFile("classes/delegation4.kt");
    }

    public void testInheritanceAndDelegationTyped() {
        blackBoxFile("classes/typedDelegation.kt");
    }

    public void testDelegationMethodsWithArgs() {
        blackBoxFile("classes/delegationMethodsWithArgs.kt");
    }

    public void testDelegationGenericArg() {
        blackBoxFile("classes/delegationGenericArg.kt");
    }

    public void testDelegationGenericLongArg() {
        blackBoxFile("classes/delegationGenericLongArg.kt");
    }

    public void testDelegationGenericArgUpperBound() {
        blackBoxFile("classes/delegationGenericArgUpperBound.kt");
    }

    public void testFunDelegation() {
        blackBoxFile("classes/funDelegation.kt");
    }

    public void testPropertyDelegation() {
        blackBoxFile("classes/propertyDelegation.kt");
    }

    public void testDiamondInheritance() {
        blackBoxFile("classes/diamondInheritance.kt");
    }

    public void testRightHandOverride() {
        blackBoxFile("classes/rightHandOverride.kt");
    }

    public void testNewInstanceExplicitConstructor() throws Exception {
        loadFile("classes/newInstanceDefaultConstructor.kt");
        final Method method = generateFunction("test");
        final Integer returnValue = (Integer) method.invoke(null);
        assertEquals(610, returnValue.intValue());
    }

    public void testInnerClass() {
        blackBoxFile("classes/innerClass.kt");
    }

    public void testInheritedInnerClass() {
        blackBoxFile("classes/inheritedInnerClass.kt");
    }

    public void testKt2532() {
        blackBoxFile("classes/kt2532.kt");
    }

    public void testInitializerBlock() {
        blackBoxFile("classes/initializerBlock.kt");
    }

    public void testAbstractMethod() throws Exception {
        loadText("abstract class Foo { abstract fun x(): String; fun y(): Int = 0 }");
        final Class aClass = generateClass("Foo");
        assertNotNull(aClass.getMethod("x"));
        assertNotNull(findDeclaredMethodByName(aClass, "y"));
    }

    public void testInheritedMethod() {
        blackBoxFile("classes/inheritedMethod.kt");
    }

    public void testInitializerBlockDImpl() {
        blackBoxFile("classes/initializerBlockDImpl.kt");
    }

    public void testPropertyInInitializer() {
        blackBoxFile("classes/propertyInInitializer.kt");
    }

    public void testOuterThis() {
        blackBoxFile("classes/outerThis.kt");
    }

    public void testExceptionConstructor() {
        blackBoxFile("classes/exceptionConstructor.kt");
    }

    public void testSimpleBox() {
        blackBoxFile("classes/simpleBox.kt");
    }

    public void testAbstractClass() throws Exception {
        loadText("abstract class SimpleClass() { }");
        final Class aClass = generateClass("SimpleClass");
        assertTrue((aClass.getModifiers() & Modifier.ABSTRACT) != 0);
    }

    public void testClassObject() {
        blackBoxFile("classes/classObject.kt");
    }

    public void testClassObjectInTrait() {
        blackBoxFile("classes/classObjectInTrait.kt");
    }

    public void testClassObjectInterface() throws Exception {
        loadFile("classes/classObjectInterface.kt");
        final Method method = generateFunction();
        Object result = method.invoke(null);
        assertInstanceOf(result, Runnable.class);
    }

    public void testOverloadBinaryOperator() {
        blackBoxFile("classes/overloadBinaryOperator.kt");
    }

    public void testOverloadUnaryOperator() {
        blackBoxFile("classes/overloadUnaryOperator.kt");
    }

    public void testOverloadPlusAssign() {
        blackBoxFile("classes/overloadPlusAssign.kt");
    }

    public void testOverloadPlusAssignReturn() {
        blackBoxFile("classes/overloadPlusAssignReturn.kt");
    }

    public void testOverloadPlusToPlusAssign() {
        blackBoxFile("classes/overloadPlusToPlusAssign.kt");
    }

    public void testEnumClass() throws Exception {
        loadText("enum class Direction { NORTH; SOUTH; EAST; WEST }");
        final Class direction = generateClass("Direction");
        final Field north = direction.getField("NORTH");
        assertEquals(direction, north.getType());
        assertInstanceOf(north.get(null), direction);
    }

    public void testEnumConstantConstructors() throws Exception {
        loadText("enum class Color(val rgb: Int) { RED: Color(0xFF0000); GREEN: Color(0x00FF00); }");
        final Class colorClass = generateClass("Color");
        final Field redField = colorClass.getField("RED");
        final Object redValue = redField.get(null);
        final Method rgbMethod = colorClass.getMethod("getRgb");
        assertEquals(0xFF0000, rgbMethod.invoke(redValue));
    }

    public void testClassObjFields() {
        blackBoxFile("classes/classObjectField.kt");
    }

    public void testPrivateOuterProperty() {
        blackBoxFile("classes/privateOuterProperty.kt");
    }

    public void testPrivateOuterFunctions() {
        blackBoxFile("classes/privateOuterFunctions.kt");
    }

    public void testKt249() {
        blackBoxFile("regressions/kt249.kt");
    }

    public void testKt48() {
        blackBoxFile("regressions/kt48.kt");
    }

    public void testKt309() {
        loadText("fun box() = null");
        final Method method = generateFunction("box");
        assertEquals(method.getReturnType().getName(), "java.lang.Object");
    }

    public void testKt343() {
        blackBoxFile("regressions/kt343.kt");
    }

    public void testKt508() {
        blackBoxFile("regressions/kt508.kt");
    }

    public void testKt504() {
        blackBoxFile("regressions/kt504.kt");
    }

    public void testKt501() {
        blackBoxFile("regressions/kt501.kt");
    }

    public void testKt496() {
        blackBoxFile("regressions/kt496.kt");
    }

    public void testKt500() {
        blackBoxFile("regressions/kt500.kt");
    }

    public void testKt285() {
        blackBoxFile("regressions/kt285.kt");
    }

    public void testKt707() {
        blackBoxFile("regressions/kt707.kt");
    }

    public void testKt903() {
        blackBoxFile("regressions/kt903.kt");
    }

    public void testKt940() {
        blackBoxFile("regressions/kt940.kt");
    }

    public void testKt1018() {
        blackBoxFile("regressions/kt1018.kt");
    }

    public void testKt1120() {
        blackBoxFile("regressions/kt1120.kt");
    }

    public void testSelfCreate() {
        blackBoxFile("classes/selfcreate.kt");
    }

    public void testKt1134() {
        blackBoxFile("regressions/kt1134.kt");
    }

    public void testKt1157() {
        blackBoxFile("regressions/kt1157.kt");
    }

    public void testKt471() {
        blackBoxFile("regressions/kt471.kt");
    }

    /*
    public void testKt1213() {
        //        blackBoxFile("regressions/kt1213.kt");
    }
    */

    public void testKt723() {
        blackBoxFile("regressions/kt723.kt");
    }

    public void testKt725() {
        blackBoxFile("regressions/kt725.kt");
    }

    public void testKt633() {
        blackBoxFile("regressions/kt633.kt");
    }

    public void testKt1345() {
        blackBoxFile("regressions/kt1345.kt");
    }

    public void testKt1538() {
        blackBoxFile("regressions/kt1538.kt");
    }

    public void testKt1759() {
        blackBoxFile("regressions/kt1759.kt");
    }

    public void testResolveOrder() {
        blackBoxFile("classes/resolveOrder.kt");
    }

    public void testKt1918() {
        blackBoxFile("regressions/kt1918.kt");
    }

    public void testKt1247() {
        blackBoxFile("regressions/kt1247.kt");
    }

    public void testKt1980() {
        blackBoxFile("regressions/kt1980.kt");
    }

    public void testKt1578() {
        blackBoxFile("regressions/kt1578.kt");
    }

    public void testKt1726() {
        blackBoxFile("regressions/kt1726.kt");
    }

    public void testKt1721() {
        blackBoxFile("regressions/kt1721.kt");
    }

    public void testKt1976() {
        blackBoxFile("regressions/kt1976.kt");
    }

    public void testKt1439() {
        blackBoxFile("regressions/kt1439.kt");
    }

    public void testKt1611() {
        blackBoxFile("regressions/kt1611.kt");
    }

    public void testKt1891() {
        blackBoxFile("regressions/kt1891.kt");
    }

    public void testKt2224() {
        blackBoxFile("regressions/kt2224.kt");
    }

    public void testKt2384() {
        blackBoxFile("regressions/kt2384.kt");
    }

    public void testKt2390() {
        blackBoxFile("regressions/kt2390.kt");
    }

    public void testKt2391() {
        blackBoxFile("regressions/kt2391.kt");
    }

    public void testKt2060() {
        blackBoxMultiFile("regressions/kt2060_1.kt", "regressions/kt2060.kt");
    }

    public void testKt2395() {
        blackBoxFile("regressions/kt2395.kt");
    }

    public void testKt2566() {
        blackBoxFile("regressions/kt2566.kt");
    }

    public void testKt2566_2() {
        blackBoxFile("regressions/kt2566_2.kt");
    }

    public void testKt2477() {
        blackBoxFile("regressions/kt2477.kt");
    }

    public void testKt2485() {
        blackBoxFile("regressions/kt2485.kt");
    }

    public void testKt2482() {
        blackBoxFile("regressions/kt2482.kt");
    }

    public void testKt2288() {
        blackBoxFile("regressions/kt2288.kt");
    }

    public void testKt2257() {
        blackBoxMultiFile("regressions/kt2257_1.kt", "regressions/kt2257_2.kt");
    }

    public void testKt1845() {
        blackBoxMultiFile("regressions/kt1845_1.kt", "regressions/kt1845_2.kt");
    }

    public void testKt2417() {
        blackBoxFile("regressions/kt2417.kt");
    }

    public void testKt2480() {
        blackBoxFile("regressions/kt2480.kt");
    }

    public void testKt454() {
        blackBoxFile("regressions/kt454.kt");
    }

    public void testKt1535() {
        blackBoxFile("regressions/kt1535.kt");
    }

    public void testKt2711() {
        blackBoxFile("regressions/kt2711.kt");
    }

    public void testKt2626() {
        blackBoxFile("regressions/kt2626.kt");
    }

    public void testKt2607() {
        blackBoxFile("regressions/kt2607.kt");
    }

    public void testNoClassObjectForJavaClass() {
        blackBoxFileWithJava("stdlib/noClassObjectForJavaClass.kt");
    }
}
