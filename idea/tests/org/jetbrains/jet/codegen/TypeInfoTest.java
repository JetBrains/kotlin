package org.jetbrains.jet.codegen;

import jet.JetObject;
import jet.typeinfo.TypeInfo;

import java.lang.reflect.Method;

/**
 * @author yole
 */
public class TypeInfoTest extends CodegenTestCase {
    @Override
    protected String getPrefix() {
        return "typeInfo";
    }

    public void testGetTypeInfo() throws Exception {
        loadFile();
        Method foo = generateFunction();
        JetObject jetObject = (JetObject) foo.invoke(null);
        TypeInfo<?> typeInfo = jetObject.getTypeInfo();
        assertNotNull(typeInfo);
    }

    public void testTypeOfOperator() throws Exception {
        loadFile();
        Method foo = generateFunction();
        TypeInfo typeInfo = (TypeInfo) foo.invoke(null);
        assertNotNull(typeInfo);
    }

    public void testAsSafeOperator() throws Exception {
        loadText("fun foo(x: Any) = x as? Runnable");
        System.out.println(generateToText());
        Method foo = generateFunction();
        assertNull(foo.invoke(null, new Object()));
        Runnable r = newRunnable();
        assertSame(r, foo.invoke(null, r));
    }

    public void testIsOperator() throws Exception {
        loadText("fun foo(x: Any) = x is Runnable");
        Method foo = generateFunction();
        assertFalse((Boolean) foo.invoke(null, new Object()));
        assertTrue((Boolean) foo.invoke(null, newRunnable()));
    }

    public void testIsWithGenerics() throws Exception {
        loadFile();
        System.out.println(generateToText());
        Method foo = generateFunction();
        assertFalse((Boolean) foo.invoke(null));
    }

    public void testNotIsOperator() throws Exception {
        loadText("fun foo(x: Any) = x !is Runnable");
        Method foo = generateFunction();
        assertTrue((Boolean) foo.invoke(null, new Object()));
        assertFalse((Boolean) foo.invoke(null, newRunnable()));
    }

    public void testIsWithGenericParameters() throws Exception {
        loadFile();
        Method foo = generateFunction();
        assertFalse((Boolean) foo.invoke(null));
    }

    private Runnable newRunnable() {
        return new Runnable() {
            @Override
            public void run() {
            }
        };
    }
}

