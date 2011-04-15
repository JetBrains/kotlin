package org.jetbrains.jet.codegen;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author yole
 */
public class ControlStructuresTest extends CodegenTestCase {
    public void testWhile() throws Exception {
        factorialTest("while.jet");
    }

    public void testDoWhile() throws Exception {
        factorialTest("doWhile.jet");
    }

    public void testBreak() throws Exception {
        factorialTest("break.jet");
    }

    private void factorialTest(final String name) throws IllegalAccessException, InvocationTargetException {
        loadFile(name);

        System.out.println(generateToText());
        final Method main = generateFunction();
        assertEquals(6, main.invoke(null, 3));
        assertEquals(120, main.invoke(null, 5));
    }

    public void testContinue() throws Exception {
        loadFile("continue.jet");
        System.out.println(generateToText());
        final Method main = generateFunction();
        assertEquals(3, main.invoke(null, 4));
        assertEquals(7, main.invoke(null, 5));
    }

    public void testIfNoElse() throws Exception {
        loadFile("ifNoElse.jet");
        System.out.println(generateToText());
        final Method main = generateFunction();
        assertEquals(5, main.invoke(null, 5, true));
        assertEquals(10, main.invoke(null, 5, false));
    }

    public void testCondJumpOnStack() throws Exception {
        loadText("fun foo(a: String): Int = if (Boolean.parseBoolean(a)) 5 else 10");
        final Method main = generateFunction();
        assertEquals(5, main.invoke(null, "true"));
        assertEquals(10, main.invoke(null, "false"));
    }
}
