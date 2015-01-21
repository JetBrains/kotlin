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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.name.SpecialNames;
import org.jetbrains.kotlin.test.ConfigurationKind;
import org.jetbrains.org.objectweb.asm.Opcodes;

import java.lang.reflect.*;

import static org.jetbrains.kotlin.codegen.CodegenTestUtil.*;

public class PropertyGenTest extends CodegenTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
    }

    @NotNull
    @Override
    protected String getPrefix() {
        return "properties";
    }

    public void testPrivateVal() throws Exception {
        loadFile();
        generateClass("PrivateVal").getDeclaredField("prop");
    }

    public void testPrivateVar() throws Exception {
        loadFile();
        Class<?> aClass = generateClass("PrivateVar");
        Object instance = aClass.newInstance();
        Method setter = findDeclaredMethodByName(aClass, "setValueOfX");
        setter.invoke(instance, 239);
        Method getter = findDeclaredMethodByName(aClass, "getValueOfX");
        assertEquals(239, ((Integer) getter.invoke(instance)).intValue());
    }

    public void testPublicVar() throws Exception {
        loadText("class PublicVar() { public var foo : Int = 0; }");
        Class<?> aClass = generateClass("PublicVar");
        Object instance = aClass.newInstance();
        Method setter = findDeclaredMethodByName(aClass, "setFoo");
        setter.invoke(instance, 239);
        Method getter = findDeclaredMethodByName(aClass, "getFoo");
        assertEquals(239, ((Integer) getter.invoke(instance)).intValue());
    }

    public void testAccessorsInInterface() {
        loadText("class AccessorsInInterface() { public var foo : Int = 0; }");
        Class<?> aClass = generateClass("AccessorsInInterface");
        assertNotNull(findDeclaredMethodByName(aClass, "getFoo"));
        assertNotNull(findDeclaredMethodByName(aClass, "setFoo"));
    }

    public void testPrivatePropertyInPackage() throws Exception {
        loadText("private val x = 239");
        Class<?> nsClass = generatePackagePartClass();
        Field[] fields = nsClass.getDeclaredFields();
        assertEquals(1, fields.length);
        Field field = fields[0];
        field.setAccessible(true);
        assertEquals("x", field.getName());
        assertEquals(Modifier.STATIC | Modifier.FINAL, field.getModifiers());
        assertEquals(239, field.get(null));
    }

    public void testFieldPropertyAccess() throws Exception {
        loadFile("properties/fieldPropertyAccess.kt");
        Method method = generateFunction("increment");
        assertEquals(1, method.invoke(null));
        assertEquals(2, method.invoke(null));
    }

    public void testFieldGetter() throws Exception {
        loadText("val now: Long get() = System.currentTimeMillis(); fun foo() = now");
        Method method = generateFunction("foo");
        assertIsCurrentTime((Long) method.invoke(null));
    }

    public void testFieldSetter() throws Exception {
        loadFile();
        Method method = generateFunction("append");
        method.invoke(null, "IntelliJ ");
        String value = (String) method.invoke(null, "IDEA");
        if (!value.equals("IntelliJ IDEA")) {
            System.out.println(generateToText());
            throw new AssertionError(value);
        }
        assertEquals("IntelliJ IDEA", value);
    }

    public void testFieldSetterPlusEq() throws Exception {
        loadFile();
        Method method = generateFunction("append");
        method.invoke(null, "IntelliJ ");
        String value = (String) method.invoke(null, "IDEA");
        assertEquals("IntelliJ IDEA", value);
    }

    public void testAccessorsWithoutBody() throws Exception {
        loadText("class AccessorsWithoutBody() { protected var foo: Int = 349\n get\n  private set\n fun setter() { foo = 610; } } ");
        Class<?> aClass = generateClass("AccessorsWithoutBody");
        Object instance = aClass.newInstance();
        Method getFoo = findDeclaredMethodByName(aClass, "getFoo");
        getFoo.setAccessible(true);
        assertTrue((getFoo.getModifiers() & Modifier.PROTECTED) != 0);
        assertEquals(349, getFoo.invoke(instance));
        Method setFoo = findDeclaredMethodByName(aClass, "setFoo");
        setFoo.setAccessible(true);
        assertTrue((setFoo.getModifiers() & Modifier.PRIVATE) != 0);
        Method setter = findDeclaredMethodByName(aClass, "setter");
        setter.invoke(instance);
        assertEquals(610, getFoo.invoke(instance));
    }

    public void testInitializersForTopLevelProperties() throws Exception {
        loadText("val x = System.currentTimeMillis()");
        Method method = generateFunction("getX");
        method.setAccessible(true);
        assertIsCurrentTime((Long) method.invoke(null));
    }

    public void testPropertyReceiverOnStack() throws Exception {
        loadFile();
        Class<?> aClass = generateClass("Evaluator");
        Constructor constructor = aClass.getConstructor(StringBuilder.class);
        StringBuilder sb = new StringBuilder("xyzzy");
        Object instance = constructor.newInstance(sb);
        Method method = aClass.getMethod("evaluateArg");
        Integer result = (Integer) method.invoke(instance);
        assertEquals(5, result.intValue());
    }

    public void testAbstractVal() throws Exception {
        loadText("abstract class Foo { public abstract val x: String }");
        Class<?> aClass = generateClass("Foo");
        assertNotNull(aClass.getMethod("getX"));
    }

    public void testKt160() throws Exception {
        loadText("internal val s = java.lang.Double.toString(1.0)");
        Method method = generateFunction("getS");
        method.setAccessible(true);
        assertEquals(method.invoke(null), "1.0");
    }

    public void testKt1846() {
        loadFile("regressions/kt1846.kt");
        Class<?> aClass = generateClass("A");
        try {
            aClass.getMethod("getV1");
            System.out.println(generateToText());
            fail();
        }
        catch (NoSuchMethodException e) {
            try {
                aClass.getMethod("setV1");
                System.out.println(generateToText());
                fail();
            }
            catch (NoSuchMethodException ee) {
                // ok
            }
        }
    }

    public void testKt2589() throws Exception {
        loadFile("regressions/kt2589.kt");
        Class<?> aClass = generateClass("Foo");
        assertTrue((aClass.getModifiers() & Opcodes.ACC_FINAL) == 0);

        Field foo = aClass.getDeclaredField("foo");
        assertTrue((foo.getModifiers() & Opcodes.ACC_PRIVATE) != 0);
        assertTrue((foo.getModifiers() & Opcodes.ACC_FINAL) == 0);

        Field bar = aClass.getDeclaredField("bar");
        assertTrue((bar.getModifiers() & Opcodes.ACC_PRIVATE) != 0);
        assertTrue((bar.getModifiers() & Opcodes.ACC_FINAL) != 0);

        Method getFoo = aClass.getDeclaredMethod("getFoo");
        assertTrue((getFoo.getModifiers() & Opcodes.ACC_PUBLIC) != 0);
        assertTrue((getFoo.getModifiers() & Opcodes.ACC_FINAL) != 0);

        Method getBar = aClass.getDeclaredMethod("getBar");
        assertTrue((getBar.getModifiers() & Opcodes.ACC_PROTECTED) != 0);
        assertTrue((getBar.getModifiers() & Opcodes.ACC_FINAL) == 0);
    }

    public void testKt2677() throws Exception {
        loadFile("regressions/kt2677.kt");
        Class<?> derived = generateClass("DerivedWeatherReport");
        Class<?> weatherReport = derived.getSuperclass();

        {
            Method get = derived.getDeclaredMethod("getForecast");
            Type type = get.getGenericReturnType();
            assertInstanceOf(type, ParameterizedType.class);
            ParameterizedType parameterizedType = (ParameterizedType) type;
            assertEquals(String.class, parameterizedType.getActualTypeArguments()[0]);

            Method set = derived.getDeclaredMethod("setForecast", (Class<?>) parameterizedType.getRawType());
            type = set.getGenericParameterTypes()[0];
            parameterizedType = (ParameterizedType) type;
            assertInstanceOf(type, ParameterizedType.class);
            assertEquals(String.class, parameterizedType.getActualTypeArguments()[0]);
        }
        {
            Method get = weatherReport.getDeclaredMethod("getForecast");
            Type type = get.getGenericReturnType();
            assertInstanceOf(type, ParameterizedType.class);
            ParameterizedType parameterizedType = (ParameterizedType) type;
            assertEquals(String.class, parameterizedType.getActualTypeArguments()[0]);

            Method set = weatherReport.getDeclaredMethod("setForecast", (Class<?>) parameterizedType.getRawType());
            type = set.getGenericParameterTypes()[0];
            parameterizedType = (ParameterizedType) type;
            assertInstanceOf(type, ParameterizedType.class);
            assertEquals(String.class, parameterizedType.getActualTypeArguments()[0]);
        }
    }

    public void testPrivateClassPropertyAccessors() throws Exception {
        loadFile("properties/privateClassPropertyAccessors.kt");
        Class<?> c = generateClass("C");
        findDeclaredMethodByName(c, "getValWithGet");
        findDeclaredMethodByName(c, "getVarWithGetSet");
        findDeclaredMethodByName(c, "setVarWithGetSet");
        findDeclaredMethodByName(c, "getDelegated");
        findDeclaredMethodByName(c, "setDelegated");
        findDeclaredMethodByName(c, "getExtension");
        findDeclaredMethodByName(c, "setExtension");

        findDeclaredMethodByName(initializedClassLoader.loadClass("C$" + SpecialNames.DEFAULT_NAME_FOR_DEFAULT_OBJECT.asString()), "getClassObjectVal");

        assertNull("Property should not have a getter", findDeclaredMethodByNameOrNull(c, "getVarNoAccessors"));
        assertNull("Property should not have a setter", findDeclaredMethodByNameOrNull(c, "setVarNoAccessors"));
    }
}
