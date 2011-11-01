package org.jetbrains.jet.codegen;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author alex.tkachman
 */
public class VarArgTest extends CodegenTestCase {
    public void testStringArray () throws InvocationTargetException, IllegalAccessException {
        loadText("fun test(vararg ts: String) = ts");
        System.out.println(generateToText());
        final Method main = generateFunction();
        String[] args = {"mama", "papa"};
        assertTrue(args == main.invoke(null, new Object[]{ args } ));
    }

    public void testIntArray () throws InvocationTargetException, IllegalAccessException {
        loadText("fun test(vararg ts: Int) = ts");
        System.out.println(generateToText());
        final Method main = generateFunction();
        int[] args = {3, 4};
        assertTrue(args == main.invoke(null, new Object[]{ args }));
    }

    public void testIntArrayKotlinNoArgs () throws InvocationTargetException, IllegalAccessException {
        loadText("fun test() = testf(); fun testf(vararg ts: Int) = ts");
        System.out.println(generateToText());
        final Method main = generateFunction();
        Object res = main.invoke(null, new Object[]{});
        assertTrue(((int[])res).length == 0);
    }

    public void testIntArrayKotlin () throws InvocationTargetException, IllegalAccessException {
        loadText("fun test() = testf(239, 7); fun testf(vararg ts: Int) = ts");
        System.out.println(generateToText());
        final Method main = generateFunction();
        Object res = main.invoke(null, new Object[]{});
        assertTrue(((int[])res).length == 2);
        assertTrue(((int[])res)[0] == 239);
        assertTrue(((int[])res)[1] == 7);
    }

    public void testNullableIntArrayKotlin () throws InvocationTargetException, IllegalAccessException {
        loadText("fun test() = testf(239.byt, 7.byt); fun testf(vararg ts: Byte?) = ts");
        System.out.println(generateToText());
        final Method main = generateFunction();
        Object res = main.invoke(null, new Object[]{});
        assertTrue(((Byte[])res).length == 2);
        assertTrue(((Byte[])res)[0] == (byte)239);
        assertTrue(((Byte[])res)[1] == 7);
    }

    public void testIntArrayKotlinObj () throws InvocationTargetException, IllegalAccessException {
        loadText("fun test() = testf(\"239\"); fun testf(vararg ts: String) = ts");
        System.out.println(generateToText());
        final Method main = generateFunction();
        Object res = main.invoke(null, new Object[]{});
        assertTrue(((String[])res).length == 1);
        assertTrue(((String[])res)[0].equals("239"));
    }
}
