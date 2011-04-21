package org.jetbrains.jet.codegen;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * @author yole
 */
public class PropertyGenTest extends CodegenTestCase {
    public void testPrivateVal() throws Exception {
        loadFile("privateVal.jet");
        System.out.println(generateToText());
        final Class aClass = loadImplementationClass(generateClassesInFile(), "PrivateVal");
        final Field[] fields = aClass.getDeclaredFields();
        assertEquals(1, fields.length);
        final Field field = fields[0];
        assertEquals("prop", field.getName());
    }

    public void testPrivateVar() throws Exception {
        loadFile("privateVar.jet");
        System.out.println(generateToText());
        final Class aClass = loadImplementationClass(generateClassesInFile(), "PrivateVar");
        final Object instance = aClass.newInstance();
        Method setter = findMethodByName(aClass, "setValueOfX");
        setter.invoke(instance, 239);
        Method getter = findMethodByName(aClass, "getValueOfX");
        assertEquals(239, ((Integer) getter.invoke(instance)).intValue());
    }

    public void testPublicVar() throws Exception {
        loadText("class PublicVar { public var foo = 0; }");
        System.out.println(generateToText());
        final Class aClass = loadImplementationClass(generateClassesInFile(), "PublicVar");
        final Object instance = aClass.newInstance();
        Method setter = findMethodByName(aClass, "setFoo");
        setter.invoke(instance, 239);
        Method getter = findMethodByName(aClass, "getFoo");
        assertEquals(239, ((Integer) getter.invoke(instance)).intValue());
    }

    public void testPropertyInNamespace() throws Exception {
        loadText("private val x = 239");
        final Class nsClass = generateNamespaceClass();
        final Field[] fields = nsClass.getDeclaredFields();
        assertEquals(1, fields.length);
        final Field field = fields[0];
        field.setAccessible(true);
        assertEquals("x", field.getName());
        assertEquals(Modifier.PRIVATE | Modifier.STATIC, field.getModifiers());
        assertEquals(239, field.get(null));
    }

    public void testFieldPropertyAccess() throws Exception {
        loadFile("fieldPropertyAccess.jet");
        final Method method = generateFunction();
        assertEquals(1, method.invoke(null));
        assertEquals(2, method.invoke(null));
    }

    public void testFieldGetter() throws Exception {
        loadText("val now: Long get() = System.currentTimeMillis(); fun foo() = now");
        final Method method = generateFunction("foo");
        assertIsCurrentTime((Long) method.invoke(null));
    }

    public void testFieldSetter() throws Exception {
        loadFile("fieldSetter.jet");
        System.out.println(generateToText());
        final Method method = generateFunction("append");
        method.invoke(null, "IntelliJ ");
        String value = (String) method.invoke(null, "IDEA");
        assertEquals(value, "IntelliJ IDEA");
    }

    public void testAccessorsWithoutBody() throws Exception {
        loadText("class AccessorsWithoutBody { public var foo: Int = 349\n  get\n  private set } ");
        final Class aClass = loadImplementationClass(generateClassesInFile(), "AccessorsWithoutBody");
        final Object instance = aClass.newInstance();
        final Method getFoo = findMethodByName(aClass, "getFoo");
        assertEquals(349, getFoo.invoke(instance));
        assertNull(findMethodByName(aClass, "setFoo"));
    }

    public void testInitializersForNamespaceProperties() throws Exception {
        loadText("public val x = System.currentTimeMillis()");
        System.out.println(generateToText());
        final Method method = generateFunction("getX");
        assertIsCurrentTime((Long) method.invoke(null));
    }
}
