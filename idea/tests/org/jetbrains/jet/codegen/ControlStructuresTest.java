package org.jetbrains.jet.codegen;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * @author yole
 */
public class ControlStructuresTest extends CodegenTestCase {
    public void testIf() throws Exception {
        loadFile("controlStructures/if.jet");

        System.out.println(generateToText());
        final Method main = generateFunction();
        assertEquals(15, main.invoke(null, true));
        assertEquals(20, main.invoke(null, false));
    }

    public void testSingleBranchIf() throws Exception {
        loadFile("controlStructures/singleBranchIf.jet");

        System.out.println(generateToText());
        final Method main = generateFunction();
        assertEquals(15, main.invoke(null, true));
        assertEquals(20, main.invoke(null, false));
    }

    public void testWhile() throws Exception {
        factorialTest("controlStructures/while.jet");
    }

    public void testDoWhile() throws Exception {
        factorialTest("controlStructures/doWhile.jet");
    }

    public void testBreak() throws Exception {
        factorialTest("controlStructures/break.jet");
    }

    private void factorialTest(final String name) throws IllegalAccessException, InvocationTargetException {
        loadFile(name);

        System.out.println(generateToText());
        final Method main = generateFunction();
        assertEquals(6, main.invoke(null, 3));
        assertEquals(120, main.invoke(null, 5));
    }

    public void testContinue() throws Exception {
        loadFile("controlStructures/continue.jet");
        System.out.println(generateToText());
        final Method main = generateFunction();
        assertEquals(3, main.invoke(null, 4));
        assertEquals(7, main.invoke(null, 5));
    }

    public void testIfNoElse() throws Exception {
        loadFile("controlStructures/ifNoElse.jet");
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

    public void testFor() throws Exception {
        loadFile("controlStructures/for.jet");
        System.out.println(generateToText());
        final Method main = generateFunction();
        List<String> args = Arrays.asList("IntelliJ", " ", "IDEA");
        assertEquals("IntelliJ IDEA", main.invoke(null, args));
    }

    public void testForInArray() throws Exception {
        loadFile("controlStructures/forInArray.jet");
        System.out.println(generateToText());
        final Method main = generateFunction();
        String[] args = new String[] { "IntelliJ", " ", "IDEA" };
        assertEquals("IntelliJ IDEA", main.invoke(null, new Object[] { args }));
    }

    public void testThrowCheckedException() throws Exception {
        loadText("fun foo() { throw new Exception(); }");
        final Method main = generateFunction();
        boolean caught = false;
        try {
            main.invoke(null);
        } catch (InvocationTargetException e) {
            if (e.getTargetException().getClass() == Exception.class) {
                caught = true;
            }
        }
        assertTrue(caught);
    }

    public void testTryCatch() throws Exception {
        loadFile("controlStructures/tryCatch.jet");
        System.out.println(generateToText());
        final Method main = generateFunction();
        assertEquals("no message", main.invoke(null, "0"));
        assertEquals("For input string: \"a\"", main.invoke(null, "a"));
    }
}
