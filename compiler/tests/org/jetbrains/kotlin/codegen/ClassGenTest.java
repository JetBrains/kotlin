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

package org.jetbrains.kotlin.codegen;

import org.jetbrains.kotlin.load.kotlin.PackageClassUtils;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.SpecialNames;
import org.jetbrains.kotlin.test.ConfigurationKind;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import static org.jetbrains.kotlin.codegen.CodegenTestUtil.findDeclaredMethodByName;

public class ClassGenTest extends CodegenTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
    }

    public void testPSVMClass() {
        loadFile("classes/simpleClass.kt");

        Class<?> aClass = generateClass("SimpleClass");
        Method[] methods = aClass.getDeclaredMethods();
        // public int SimpleClass.foo()
        assertEquals(1, methods.length);
    }

    public void testArrayListInheritance() throws Exception {
        loadFile("classes/inheritingFromArrayList.kt");
        Class<?> aClass = generateClass("Foo");
        assertInstanceOf(aClass.newInstance(), List.class);
    }

    public void testDelegationToVal() throws Exception {
        loadFile("classes/delegationToVal.kt");
        GeneratedClassLoader loader = generateAndCreateClassLoader();
        Class<?> aClass = loader.loadClass(PackageClassUtils.getPackageClassName(FqName.ROOT));
        assertEquals("OK", aClass.getMethod("box").invoke(null));

        Class<?> test = loader.loadClass("Test");
        try {
            test.getDeclaredField("$delegate_0");
            fail("$delegate_0 field generated for class Test but should not");
        }
        catch (NoSuchFieldException e) {
            // ok
        }

        Class<?> test2 = loader.loadClass("Test2");
        try {
            test2.getDeclaredField("$delegate_0");
            fail("$delegate_0 field generated for class Test2 but should not");
        }
        catch (NoSuchFieldException e) {
            // ok
        }

        Class<?> test3 = loader.loadClass("Test3");
        Class<?> iActing = loader.loadClass("IActing");
        Object obj = test3.newInstance();
        assertTrue(iActing.isInstance(obj));
        Method iActingMethod = iActing.getMethod("act");
        assertEquals("OK", iActingMethod.invoke(obj));
        assertEquals("OKOK", iActingMethod.invoke(test3.getMethod("getActing").invoke(obj)));
    }

    public void testNewInstanceExplicitConstructor() throws Exception {
        loadFile("classes/newInstanceDefaultConstructor.kt");
        Method method = generateFunction("test");
        Integer returnValue = (Integer) method.invoke(null);
        assertEquals(610, returnValue.intValue());
    }

    public void testAbstractMethod() throws Exception {
        loadText("abstract class Foo { abstract fun x(): String; fun y(): Int = 0 }");
        Class<?> aClass = generateClass("Foo");
        assertNotNull(aClass.getMethod("x"));
        findDeclaredMethodByName(aClass, "y");
    }

    public void testAbstractClass() throws Exception {
        loadText("abstract class SimpleClass() { }");
        Class<?> aClass = generateClass("SimpleClass");
        assertTrue((aClass.getModifiers() & Modifier.ABSTRACT) != 0);
    }

    public void testClassObjectInterface() throws Exception {
        loadFile("classes/classObjectInterface.kt");
        Method method = generateFunction();
        Object result = method.invoke(null);
        assertInstanceOf(result, Runnable.class);
    }

    public void testEnumClass() throws Exception {
        loadText("enum class Direction { NORTH; SOUTH; EAST; WEST }");
        Class<?> direction = generateClass("Direction");
        Field north = direction.getField("NORTH");
        assertEquals(direction, north.getType());
        assertInstanceOf(north.get(null), direction);
    }

    public void testEnumConstantConstructors() throws Exception {
        loadText("enum class Color(val rgb: Int) { RED: Color(0xFF0000); GREEN: Color(0x00FF00); }");
        Class<?> colorClass = generateClass("Color");
        Field redField = colorClass.getField("RED");
        Object redValue = redField.get(null);
        Method rgbMethod = colorClass.getMethod("getRgb");
        assertEquals(0xFF0000, rgbMethod.invoke(redValue));
    }

    public void testKt309() {
        loadText("fun box() = null");
        Method method = generateFunction("box");
        assertEquals(method.getReturnType().getName(), "java.lang.Void");
    }

    /*
    public void testKt1213() {
        //        blackBoxFile("regressions/kt1213.kt");
    }
    */

    public void testClassObjectIsInnerClass() throws Exception {
        loadFile("classes/classObjectIsInnerClass.kt");
        GeneratedClassLoader loader = generateAndCreateClassLoader();
        Class<?> a = loader.loadClass("A");
        Class<?> defaultObject = loader.loadClass("A$" + SpecialNames.DEFAULT_NAME_FOR_DEFAULT_OBJECT.asString());
        assertSameElements(a.getDeclaredClasses(), defaultObject);
        assertEquals(a, defaultObject.getDeclaringClass());
    }
}
