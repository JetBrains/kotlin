package org.jetbrains.jet.codegen;

import jet.JetObject;
import jet.TypeCastException;
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
        Method foo = generateFunction();
        assertNull(foo.invoke(null, new Object()));
        Runnable r = newRunnable();
        assertSame(r, foo.invoke(null, r));
    }

    public void testAsOperator() throws Exception {
        loadText("fun foo(x: Any) = x as Runnable");
        Method foo = generateFunction();
        Runnable r = newRunnable();
        assertSame(r, foo.invoke(null, r));
        assertThrows(foo, TypeCastException.class, null, new Object());
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

    public void testIsTypeParameter() throws Exception {
        blackBoxFile("typeInfo/isTypeParameter.jet");
    }

    public void testAsSafeWithGenerics() throws Exception {
        loadFile();
        Method foo = generateFunction();
        assertNull(foo.invoke(null));
    }

    public void testAsInLoop() throws Exception {
        loadFile();
        generateFunction();  // assert no exception
    }

    public void testPrimitiveTypeInfo() throws Exception {
        blackBoxFile("typeInfo/primitiveTypeInfo.jet");
    }

    public void testNullability() throws Exception {
        blackBoxFile("typeInfo/nullability.jet");
    }

    public void testGenericFunction() throws Exception {
        blackBoxFile("typeInfo/genericFunction.jet");
    }

    public void testForwardTypeParameter() throws Exception {
        blackBoxFile("typeInfo/forwardTypeParameter.jet");
    }

    public void testClassObjectInTypeInfo() throws Exception {
        loadFile();
        Method foo = generateFunction();
        JetObject jetObject = (JetObject) foo.invoke(null);
        TypeInfo<?> typeInfo = jetObject.getTypeInfo();
        final Object object = typeInfo.getClassObject();
        final Method classObjFoo = object.getClass().getMethod("foo");
        assertNotNull(classObjFoo);
    }

    private Runnable newRunnable() {
        return new Runnable() {
            @Override
            public void run() {
            }
        };
    }
}

