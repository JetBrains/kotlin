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
import org.jetbrains.kotlin.test.FirParser;
import org.jetbrains.org.objectweb.asm.Opcodes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.*;

import static org.jetbrains.kotlin.codegen.CodegenTestUtil.findDeclaredMethodByName;
import static org.jetbrains.kotlin.codegen.CodegenTestUtil.findDeclaredMethodByNameOrNull;
import static org.junit.jupiter.api.Assertions.*;

public class PropertyGenTest extends CodegenTestCase {
    @Override
    public @NotNull FirParser getFirParser() {
        return FirParser.LightTree;
    }

    @BeforeEach
    void setUp() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
    }

    @NotNull
    @Override
    protected String getPrefix() {
        return "properties";
    }

    @Test
    public void testPrivateVal() throws Exception {
        loadFile();
        generateClass("PrivateVal").getDeclaredField("prop");
    }

    @Test
    public void testPrivateVar() throws Exception {
        loadFile();
        Class<?> aClass = generateClass("PrivateVar");
        Object instance = aClass.newInstance();
        Method setter = findDeclaredMethodByName(aClass, "setValueOfX");
        setter.invoke(instance, 239);
        Method getter = findDeclaredMethodByName(aClass, "getValueOfX");
        assertEquals(239, ((Integer) getter.invoke(instance)).intValue());
    }

    @Test
    public void testPublicVar() throws Exception {
        loadText("class PublicVar() { public var foo : Int = 0; }");
        Class<?> aClass = generateClass("PublicVar");
        Object instance = aClass.newInstance();
        Method setter = findDeclaredMethodByName(aClass, "setFoo");
        setter.invoke(instance, 239);
        Method getter = findDeclaredMethodByName(aClass, "getFoo");
        assertEquals(239, ((Integer) getter.invoke(instance)).intValue());
    }

    @Test
    public void testAccessorsInInterface() {
        loadText("class AccessorsInInterface() { public var foo : Int = 0; }");
        Class<?> aClass = generateClass("AccessorsInInterface");
        assertNotNull(findDeclaredMethodByName(aClass, "getFoo"));
        assertNotNull(findDeclaredMethodByName(aClass, "setFoo"));
    }

    @Test
    public void testPrivatePropertyInPackage() throws Exception {
        loadText("private val x = 239");
        Class<?> nsClass = generateFacadeClass();
        Field[] fields = nsClass.getDeclaredFields();
        assertEquals(1, fields.length);
        Field field = fields[0];
        field.setAccessible(true);
        assertEquals("x", field.getName());
        assertEquals(Modifier.STATIC | Modifier.FINAL | Modifier.PRIVATE, field.getModifiers());
        assertEquals(239, field.get(null));
    }

    @Test
    public void testFieldPropertyAccess() throws Exception {
        loadFile();
        Method method = generateFunction("increment");
        assertEquals(1, method.invoke(null));
        assertEquals(2, method.invoke(null));
    }

    @Test
    public void testFieldGetter() throws Exception {
        loadText("val now: Long get() = 42L; fun foo() = now");
        Method method = generateFunction("foo");
        assertEquals(Long.valueOf(42), method.invoke(null));
    }

    @Test
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

    @Test
    public void testFieldSetterPlusEq() throws Exception {
        loadFile();
        Method method = generateFunction("append");
        method.invoke(null, "IntelliJ ");
        String value = (String) method.invoke(null, "IDEA");
        assertEquals("IntelliJ IDEA", value);
    }

    @Test
    public void testAccessorsWithoutBody() throws Exception {
        loadText("class AccessorsWithoutBody() { protected var foo: Int = 349\n get\n  private set\n fun setter() { foo = 610; } } ");
        Class<?> aClass = generateClass("AccessorsWithoutBody");
        Object instance = aClass.newInstance();
        Method getFoo = findDeclaredMethodByName(aClass, "getFoo");
        getFoo.setAccessible(true);
        assertTrue((getFoo.getModifiers() & Modifier.PROTECTED) != 0);
        assertEquals(349, getFoo.invoke(instance));
        assertNull(findDeclaredMethodByNameOrNull(aClass, "setFoo"));
        Method setter = findDeclaredMethodByName(aClass, "setter");
        setter.invoke(instance);
        assertEquals(610, getFoo.invoke(instance));
    }

    @Test
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

    @Test
    public void testAbstractVal() throws Exception {
        loadText("abstract class Foo { public abstract val x: String }");
        Class<?> aClass = generateClass("Foo");
        assertNotNull(aClass.getMethod("getX"));
    }

    @Test
    public void testKt160() throws Exception {
        loadText("internal val s = java.lang.Double.toString(1.0)");
        Method method = generateFunction("getS");
        method.setAccessible(true);
        assertEquals(method.invoke(null), "1.0");
    }

    @Test
    public void testKt1846() {
        loadFile();
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

    @Test
    public void testKt2589() throws Exception {
        loadFile();
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

    @Test
    public void testKt2677() throws Exception {
        loadFile();
        Class<?> derived = generateClass("DerivedWeatherReport");
        Class<?> weatherReport = derived.getSuperclass();

        {
            Method get = derived.getDeclaredMethod("getForecast");
            Type type = get.getGenericReturnType();
            assertInstanceOf(ParameterizedType.class, type);
            ParameterizedType parameterizedType = (ParameterizedType) type;
            assertEquals(String.class, parameterizedType.getActualTypeArguments()[0]);

            Method set = derived.getDeclaredMethod("setForecast", (Class<?>) parameterizedType.getRawType());
            type = set.getGenericParameterTypes()[0];
            parameterizedType = (ParameterizedType) type;
            assertInstanceOf(ParameterizedType.class, type);
            assertEquals(String.class, parameterizedType.getActualTypeArguments()[0]);
        }
        {
            Method get = weatherReport.getDeclaredMethod("getForecast");
            Type type = get.getGenericReturnType();
            assertInstanceOf(ParameterizedType.class, type);
            ParameterizedType parameterizedType = (ParameterizedType) type;
            assertEquals(String.class, parameterizedType.getActualTypeArguments()[0]);

            Method set = weatherReport.getDeclaredMethod("setForecast", (Class<?>) parameterizedType.getRawType());
            type = set.getGenericParameterTypes()[0];
            parameterizedType = (ParameterizedType) type;
            assertInstanceOf(ParameterizedType.class, type);
            assertEquals(String.class, parameterizedType.getActualTypeArguments()[0]);
        }
    }

    @Test
    public void testPrivateClassPropertyAccessors() throws Exception {
        loadFile();
        Class<?> c = generateClass("C");
        findDeclaredMethodByName(c, "getValWithGet");
        findDeclaredMethodByName(c, "getVarWithGetSet");
        findDeclaredMethodByName(c, "setVarWithGetSet");
        findDeclaredMethodByName(c, "getDelegated");
        findDeclaredMethodByName(c, "setDelegated");
        findDeclaredMethodByName(c, "getExtension");
        findDeclaredMethodByName(c, "setExtension");

        findDeclaredMethodByName(
                initializedClassLoader.loadClass("C$" + SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT.asString()),
                "getClassObjectVal"
        );

        assertNull(findDeclaredMethodByNameOrNull(c, "getVarNoAccessors"), "Property should not have a getter");
        assertNull(findDeclaredMethodByNameOrNull(c, "setVarNoAccessors"), "Property should not have a setter");
    }
}
