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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

/**
 * @author yole
 * @author alex.tkachman
 */
public class ClassGenTest extends CodegenTestCase {
    public void testPSVMClass() throws Exception {
        loadFile("classes/simpleClass.jet");

        final Class aClass = loadClass("SimpleClass", generateClassesInFile());
        final Method[] methods = aClass.getDeclaredMethods();
        // public int SimpleClass.foo()
        // public jet.TypeInfo SimpleClass.getTypeInfo()
        assertEquals(1, methods.length);
    }

    public void testArrayListInheritance() throws Exception {
        loadFile("classes/inheritingFromArrayList.jet");
        System.out.println(generateToText());
        final Class aClass = loadClass("Foo", generateClassesInFile());
        assertInstanceOf(aClass.newInstance(), List.class);
    }

    public void testInheritanceAndDelegation_DelegatingDefaultConstructorProperties() throws Exception {
        blackBoxFile("classes/inheritance.jet");
    }

    public void testFunDelegation() throws Exception {
        blackBoxFile("classes/funDelegation.jet");
    }

    public void testPropertyDelegation() throws Exception {
        blackBoxFile("classes/propertyDelegation.jet");
    }

    public void testDiamondInheritance() throws Exception {
        blackBoxFile("classes/diamondInheritance.jet");
    }

    public void testRightHandOverride() throws Exception {
        blackBoxFile("classes/rightHandOverride.jet");
    }

    public void testNewInstanceExplicitConstructor() throws Exception {
        loadFile("classes/newInstanceDefaultConstructor.jet");
//        System.out.println(generateToText());
        final Method method = generateFunction("test");
        final Integer returnValue = (Integer) method.invoke(null);
        assertEquals(610, returnValue.intValue());
    }

    public void testInnerClass() throws Exception {
        blackBoxFile("classes/innerClass.jet");
    }

    public void testInheritedInnerClass() throws Exception {
        blackBoxFile("classes/inheritedInnerClass.jet");
    }

    public void testInitializerBlock() throws Exception {
        blackBoxFile("classes/initializerBlock.jet");
    }

    public void testAbstractMethod() throws Exception {
        loadText("abstract class Foo { abstract fun x(): String; fun y(): Int = 0 }");

        final ClassFileFactory codegens = generateClassesInFile();
        final Class aClass = loadClass("Foo", codegens);
        assertNotNull(aClass.getMethod("x"));
        assertNotNull(findMethodByName(aClass, "y"));
    }

    public void testInheritedMethod() throws Exception {
        blackBoxFile("classes/inheritedMethod.jet");
    }

    public void testInitializerBlockDImpl() throws Exception {
        blackBoxFile("classes/initializerBlockDImpl.jet");
    }

    public void testPropertyInInitializer() throws Exception {
        blackBoxFile("classes/propertyInInitializer.jet");
    }

    public void testOuterThis() throws Exception {
        blackBoxFile("classes/outerThis.jet");
    }

    public void testSecondaryConstructors() throws Exception {
        blackBoxFile("classes/secondaryConstructors.jet");
    }

    public void testExceptionConstructor() throws Exception {
        blackBoxFile("classes/exceptionConstructor.jet");
    }

    public void testSimpleBox() throws Exception {
        blackBoxFile("classes/simpleBox.jet");
    }

    public void testAbstractClass() throws Exception {
        loadText("abstract class SimpleClass() { }");

        final Class aClass = createClassLoader(generateClassesInFile()).loadClass("SimpleClass");
        assertTrue((aClass.getModifiers() & Modifier.ABSTRACT) != 0);
    }

    public void testClassObject() throws Exception {
        blackBoxFile("classes/classObject.jet");
    }

    public void testClassObjectMethod() throws Exception {
// todo to be implemented after removal of type info
//        blackBoxFile("classes/classObjectMethod.jet");
    }

    public void testClassObjectInterface() throws Exception {
        loadFile("classes/classObjectInterface.jet");
        final Method method = generateFunction();
        Object result = method.invoke(null);
        assertInstanceOf(result, Runnable.class);
    }

    public void testOverloadBinaryOperator() throws Exception {
        blackBoxFile("classes/overloadBinaryOperator.jet");
    }

    public void testOverloadUnaryOperator() throws Exception {
        blackBoxFile("classes/overloadUnaryOperator.jet");
    }

    public void testOverloadPlusAssign() throws Exception {
        blackBoxFile("classes/overloadPlusAssign.jet");
    }

    public void testOverloadPlusAssignReturn() throws Exception {
        blackBoxFile("classes/overloadPlusAssignReturn.jet");
    }

    public void testOverloadPlusToPlusAssign() throws Exception {
        blackBoxFile("classes/overloadPlusToPlusAssign.jet");
    }

    public void testEnumClass() throws Exception {
        loadText("enum class Direction { NORTH; SOUTH; EAST; WEST }");
        final Class direction = createClassLoader(generateClassesInFile()).loadClass("Direction");
//        System.out.println(generateToText());
        final Field north = direction.getField("NORTH");
        assertEquals(direction, north.getType());
        assertInstanceOf(north.get(null), direction);
    }

    public void testEnumConstantConstructors() throws Exception {
        loadText("enum class Color(val rgb: Int) { RED: Color(0xFF0000); GREEN: Color(0x00FF00); }");
        final Class colorClass = createClassLoader(generateClassesInFile()).loadClass("Color");
        final Field redField = colorClass.getField("RED");
        final Object redValue = redField.get(null);
        final Method rgbMethod = colorClass.getMethod("getRgb");
        assertEquals(0xFF0000, rgbMethod.invoke(redValue));
    }

    public void testClassObjFields() throws Exception {
        loadText("class A() { class object { val value = 10 } }\n" +
                 "fun box() = if(A.value == 10) \"OK\" else \"fail\"");
        blackBox();
    }

    public void testKt249() throws Exception {
        blackBoxFile("regressions/kt249.jet");
    }

    public void testKt48 () throws Exception {
        blackBoxFile("regressions/kt48.jet");
//        System.out.println(generateToText());
    }

    public void testKt309 () throws Exception {
        loadText("fun box() = null");
        final Method method = generateFunction("box");
        assertEquals(method.getReturnType().getName(), "java.lang.Object");
//        System.out.println(generateToText());
    }

    public void testKt343 () throws Exception {
        blackBoxFile("regressions/kt343.jet");
//        System.out.println(generateToText());
    }

    public void testKt508 () throws Exception {
        loadFile("regressions/kt508.jet");
        System.out.println(generateToText());
        blackBox();
    }

    public void testKt504 () throws Exception {
        loadFile("regressions/kt504.jet");
//        System.out.println(generateToText());
        blackBox();
    }

    public void testKt501 () throws Exception {
        blackBoxFile("regressions/kt501.jet");
    }

    public void testKt496 () throws Exception {
        blackBoxFile("regressions/kt496.jet");
//        System.out.println(generateToText());
    }

    public void testKt500 () throws Exception {
        blackBoxFile("regressions/kt500.jet");
    }

    public void testKt694 () throws Exception {
//        blackBoxFile("regressions/kt694.jet");
    }

    public void testKt285 () throws Exception {
//        blackBoxFile("regressions/kt285.jet");
    }

    public void testKt707 () throws Exception {
        blackBoxFile("regressions/kt707.jet");
    }

    public void testKt857 () throws Exception {
//        blackBoxFile("regressions/kt857.jet");
    }

    public void testKt903 () throws Exception {
        blackBoxFile("regressions/kt903.jet");
    }

    public void testKt940 () throws Exception {
        blackBoxFile("regressions/kt940.kt");
    }

    public void testKt1018 () throws Exception {
        blackBoxFile("regressions/kt1018.kt");
    }

    public void testKt1120 () throws Exception {
//        createEnvironmentWithFullJdk();
//        blackBoxFile("regressions/kt1120.kt");
    }

    public void testSelfCreate() throws Exception {
        createEnvironmentWithFullJdk();
        blackBoxFile("classes/selfcreate.kt");
    }

    public void testKt1134() throws Exception {
        blackBoxFile("regressions/kt1134.kt");
    }

    public void testKt1157() throws Exception {
        blackBoxFile("regressions/kt1157.kt");
    }

    public void testKt471() throws Exception {
        blackBoxFile("regressions/kt471.kt");
    }

    public void testKt1213() throws Exception {
//        blackBoxFile("regressions/kt1213.kt");
    }

    public void testKt723() throws Exception {
        blackBoxFile("regressions/kt723.kt");
    }

    public void testKt725() throws Exception {
        blackBoxFile("regressions/kt725.kt");
    }

    public void testKt633() throws Exception {
        blackBoxFile("regressions/kt633.kt");
    }

}
