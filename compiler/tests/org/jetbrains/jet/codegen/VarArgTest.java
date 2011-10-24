package org.jetbrains.jet.codegen;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author alex.tkachman
 */
public class VarArgTest extends CodegenTestCase {
    public void testStringArray () throws InvocationTargetException, IllegalAccessException {
        /*
        loadText("fun test(vararg ts: String) = ts");
        System.out.println(generateToText());
        final Method main = generateFunction();
        Object[] args = {"mama", "papa"};
        assertTrue(args == main.invoke(null, args));
        */
    }

    public void testIntArray () throws InvocationTargetException, IllegalAccessException {
        /*
        loadText("fun test(vararg ts: Int) = ts");
        System.out.println(generateToText());
        final Method main = generateFunction();
        int[] args = {3, 4};
        assertTrue(args == main.invoke(null, args));
        */
    }
}
