package org.jetbrains.jet.codegen;

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
        loadFile();
        final Class aClass = loadImplementationClass(generateClassesInFile(), "PrivateVal");
        final Field[] fields = aClass.getDeclaredFields();
        assertEquals(2, fields.length);  // $typeInfo, prop
        final Field field = fields[1];
        assertEquals("prop", field.getName());
    }

    public void testPrivateVar() throws Exception {
        loadFile();
        final Class aClass = loadImplementationClass(generateClassesInFile(), "PrivateVar");
        final Object instance = aClass.newInstance();
        Method setter = findMethodByName(aClass, "setValueOfX");
        setter.invoke(instance, 239);
        Method getter = findMethodByName(aClass, "getValueOfX");
        assertEquals(239, ((Integer) getter.invoke(instance)).intValue());
    }

    public void testPublicVar() throws Exception {
        loadText("class PublicVar() { public var foo = 0; }");
        final Class aClass = loadImplementationClass(generateClassesInFile(), "PublicVar");
        final Object instance = aClass.newInstance();
        Method setter = findMethodByName(aClass, "setFoo");
        setter.invoke(instance, 239);
        Method getter = findMethodByName(aClass, "getFoo");
        assertEquals(239, ((Integer) getter.invoke(instance)).intValue());
    }

    public void testAccessorsInInterface() {
        loadText("class AccessorsInInterface() { public var foo = 0; }");
        final Class aClass = loadClass("AccessorsInInterface", generateClassesInFile());
        assertNotNull(findMethodByName(aClass, "getFoo"));
        assertNotNull(findMethodByName(aClass, "setFoo"));
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
        loadFile("properties/fieldPropertyAccess.jet");
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
        loadFile();
        final Method method = generateFunction("append");
        method.invoke(null, "IntelliJ ");
        String value = (String) method.invoke(null, "IDEA");
        assertEquals(value, "IntelliJ IDEA");
    }

    public void testFieldSetterPlusEq() throws Exception {
        loadFile();
        final Method method = generateFunction("append");
        method.invoke(null, "IntelliJ ");
        String value = (String) method.invoke(null, "IDEA");
        assertEquals(value, "IntelliJ IDEA");
    }

    public void testAccessorsWithoutBody() throws Exception {
        loadText("class AccessorsWithoutBody() { public var foo: Int = 349\n  get\n  private set\n fun setter() { foo = 610; } } ");
        final Class aClass = loadImplementationClass(generateClassesInFile(), "AccessorsWithoutBody");
        final Object instance = aClass.newInstance();
        final Method getFoo = findMethodByName(aClass, "getFoo");
        assertEquals(349, getFoo.invoke(instance));
        final Method setFoo = findMethodByName(aClass, "setFoo");
        assertTrue((setFoo.getModifiers() & Modifier.PRIVATE) != 0);
        final Method setter = findMethodByName(aClass,  "setter");
        setter.invoke(instance);
        assertEquals(610, getFoo.invoke(instance));
    }

    public void testInitializersForNamespaceProperties() throws Exception {
        loadText("public val x = System.currentTimeMillis()");
        final Method method = generateFunction("getX");
        assertIsCurrentTime((Long) method.invoke(null));
    }

    public void testPropertyReceiverOnStack() throws Exception {
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
        loadText("class Foo { public abstract val x: String }");
        final Codegens codegens = generateClassesInFile();
        final Class aClass = loadClass("Foo", codegens);
        assertNotNull(aClass.getMethod("getX"));
    }
}
