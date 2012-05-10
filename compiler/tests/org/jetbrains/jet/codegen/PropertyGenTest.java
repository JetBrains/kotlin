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

import org.jetbrains.jet.lang.resolve.java.CompilerSpecialMode;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * @author yole
 */
public class PropertyGenTest extends CodegenTestCase {
    @Override
    protected String getPrefix() {
        return "properties";
    }

    public void testPrivateVal() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(CompilerSpecialMode.JDK_HEADERS);
        loadFile();
        final Class aClass = loadImplementationClass(generateClassesInFile(), "PrivateVal");
        final Field[] fields = aClass.getDeclaredFields();
        assertEquals(1, fields.length);  // prop
        final Field field = fields[0];
        assertEquals("prop", field.getName());
    }

    public void testPrivateVar() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(CompilerSpecialMode.JDK_HEADERS);
        loadFile();
        final Class aClass = loadImplementationClass(generateClassesInFile(), "PrivateVar");
        final Object instance = aClass.newInstance();
        Method setter = findMethodByName(aClass, "setValueOfX");
        setter.invoke(instance, 239);
        Method getter = findMethodByName(aClass, "getValueOfX");
        assertEquals(239, ((Integer) getter.invoke(instance)).intValue());
    }

    public void testPublicVar() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(CompilerSpecialMode.JDK_HEADERS);
        loadText("class PublicVar() { public var foo : Int = 0; }");
        final Class aClass = loadImplementationClass(generateClassesInFile(), "PublicVar");
        final Object instance = aClass.newInstance();
        Method setter = findMethodByName(aClass, "setFoo");
        setter.invoke(instance, 239);
        Method getter = findMethodByName(aClass, "getFoo");
        assertEquals(239, ((Integer) getter.invoke(instance)).intValue());
    }

    public void testAccessorsInInterface() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(CompilerSpecialMode.JDK_HEADERS);
        loadText("class AccessorsInInterface() { public var foo : Int = 0; }");
        final Class aClass = loadClass("AccessorsInInterface", generateClassesInFile());
        assertNotNull(findMethodByName(aClass, "getFoo"));
        assertNotNull(findMethodByName(aClass, "setFoo"));
    }

    public void testPrivatePropertyInNamespace() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(CompilerSpecialMode.JDK_HEADERS);
        loadText("private val x = 239");
        final Class nsClass = generateNamespaceClass();
        final Field[] fields = nsClass.getDeclaredFields();
        assertEquals(1, fields.length);
        final Field field = fields[0];
        field.setAccessible(true);
        assertEquals("x", field.getName());
        assertEquals(Modifier.PRIVATE | Modifier.STATIC | Modifier.FINAL, field.getModifiers());
        assertEquals(239, field.get(null));
    }

    public void testFieldPropertyAccess() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(CompilerSpecialMode.JDK_HEADERS);
        loadFile("properties/fieldPropertyAccess.jet");
//        System.out.println(generateToText());
        final Method method = generateFunction();
        assertEquals(1, method.invoke(null));
        assertEquals(2, method.invoke(null));
    }

    public void testFieldGetter() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(CompilerSpecialMode.JDK_HEADERS);
        loadText("val now: Long get() = System.currentTimeMillis(); fun foo() = now");
        final Method method = generateFunction("foo");
        assertIsCurrentTime((Long) method.invoke(null));
    }

    public void testFieldSetter() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(CompilerSpecialMode.JDK_HEADERS);
        loadFile();
        final Method method = generateFunction("append");
        method.invoke(null, "IntelliJ ");
        String value = (String) method.invoke(null, "IDEA");
        if (!value.equals("IntelliJ IDEA")) {
            System.out.println(generateToText());
        }
        assertEquals("IntelliJ IDEA", value);
    }

    public void testFieldSetterPlusEq() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(CompilerSpecialMode.JDK_HEADERS);
        loadFile();
        final Method method = generateFunction("append");
        method.invoke(null, "IntelliJ ");
        String value = (String) method.invoke(null, "IDEA");
        assertEquals("IntelliJ IDEA", value);
    }

    public void testAccessorsWithoutBody() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(CompilerSpecialMode.JDK_HEADERS);
        loadText("class AccessorsWithoutBody() { protected var foo: Int = 349\n get\n  private set\n fun setter() { foo = 610; } } ");
//        System.out.println(generateToText());
        final Class aClass = loadImplementationClass(generateClassesInFile(), "AccessorsWithoutBody");
        final Object instance = aClass.newInstance();
        final Method getFoo = findMethodByName(aClass, "getFoo");
        getFoo.setAccessible(true);
        assertTrue((getFoo.getModifiers() & Modifier.PROTECTED) != 0);
        assertEquals(349, getFoo.invoke(instance));
        final Method setFoo = findMethodByName(aClass, "setFoo");
        setFoo.setAccessible(true);
        assertTrue((setFoo.getModifiers() & Modifier.PRIVATE) != 0);
        final Method setter = findMethodByName(aClass,  "setter");
        setter.invoke(instance);
        assertEquals(610, getFoo.invoke(instance));
    }

    public void testInitializersForNamespaceProperties() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(CompilerSpecialMode.JDK_HEADERS);
        loadText("val x = System.currentTimeMillis()");
        final Method method = generateFunction("getX");
        method.setAccessible(true);
        assertIsCurrentTime((Long) method.invoke(null));
    }

    public void testPropertyReceiverOnStack() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(CompilerSpecialMode.JDK_HEADERS);
        loadFile();
        final Class aClass = loadImplementationClass(generateClassesInFile(), "Evaluator");
        final Constructor constructor = aClass.getConstructor(StringBuilder.class);
        StringBuilder sb = new StringBuilder("xyzzy");
        final Object instance = constructor.newInstance(sb);
        final Method method = aClass.getMethod("evaluateArg");
        Integer result = (Integer) method.invoke(instance);
        assertEquals(5, result.intValue());
    }

    public void testAbstractVal() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(CompilerSpecialMode.JDK_HEADERS);
        loadText("abstract class Foo { public abstract val x: String }");
        final ClassFileFactory codegens = generateClassesInFile();
        final Class aClass = loadClass("Foo", codegens);
        assertNotNull(aClass.getMethod("getX"));
    }

    public void testVolatileProperty() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(CompilerSpecialMode.JDK_HEADERS);
        loadText("abstract class Foo { public volatile var x: String = \"\"; }");
//        System.out.println(generateToText());
        final ClassFileFactory codegens = generateClassesInFile();
        final Class aClass = loadClass("Foo", codegens);
        Field x = aClass.getDeclaredField("x");
        assertTrue((x.getModifiers() & Modifier.VOLATILE) != 0);
    }

    public void testKt257 () throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(CompilerSpecialMode.JDK_HEADERS);
        blackBoxFile("regressions/kt257.jet");
//        System.out.println(generateToText());
    }

    public void testKt613 () throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(CompilerSpecialMode.JDK_HEADERS);
        blackBoxFile("regressions/kt613.jet");
    }

    public void testKt160() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(CompilerSpecialMode.JDK_HEADERS);
        loadText("internal val s = java.lang.Double.toString(1.0)");
        final Method method = generateFunction("getS");
        method.setAccessible(true);
        assertEquals(method.invoke(null), "1.0");
    }

    public void testKt1165() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(CompilerSpecialMode.JDK_HEADERS);
        blackBoxFile("regressions/kt1165.kt");
    }

    public void testKt1168() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(CompilerSpecialMode.JDK_HEADERS);
        blackBoxFile("regressions/kt1168.kt");
    }

    public void testKt1170() throws Exception {
        createEnvironmentWithFullJdk();
        blackBoxFile("regressions/kt1170.kt");
    }

    public void testKt1159() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(CompilerSpecialMode.JDK_HEADERS);
        blackBoxFile("regressions/kt1159.kt");
    }

    public void testKt1417() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(CompilerSpecialMode.JDK_HEADERS);
        blackBoxFile("regressions/kt1417.kt");
    }

    public void testKt1398() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(CompilerSpecialMode.JDK_HEADERS);
        blackBoxFile("regressions/kt1398.kt");
    }

    public void testKt1482() throws Exception {
        createEnvironmentWithFullJdk();
        blackBoxFile("regressions/kt1482.kt");
    }
}
