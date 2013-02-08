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

    public void testNewInstanceExplicitConstructor() throws Exception {
        loadFile("classes/newInstanceDefaultConstructor.kt");
        final Method method = generateFunction("test");
        final Integer returnValue = (Integer) method.invoke(null);
        assertEquals(610, returnValue.intValue());
    }

    public void testAbstractMethod() throws Exception {
        loadText("abstract class Foo { abstract fun x(): String; fun y(): Int = 0 }");
        final Class aClass = generateClass("Foo");
        assertNotNull(aClass.getMethod("x"));
        assertNotNull(findDeclaredMethodByName(aClass, "y"));
    }

    public void testAbstractClass() throws Exception {
        loadText("abstract class SimpleClass() { }");
        final Class aClass = generateClass("SimpleClass");
        assertTrue((aClass.getModifiers() & Modifier.ABSTRACT) != 0);
    }

    public void testClassObjectInterface() throws Exception {
        loadFile("classes/classObjectInterface.kt");
        final Method method = generateFunction();
        Object result = method.invoke(null);
        assertInstanceOf(result, Runnable.class);
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

    public void testKt309() {
        loadText("fun box() = null");
        final Method method = generateFunction("box");
        assertEquals(method.getReturnType().getName(), "java.lang.Object");
    }

    /*
    public void testKt1213() {
        //        blackBoxFile("regressions/kt1213.kt");
    }
    */
}
