package org.jetbrains.jet.codegen;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author alex.tkachman
 */
public class FunctionGenTest extends CodegenTestCase {
    public void testDefaultArgs() throws Exception {
        blackBoxFile("functions/defaultargs.jet");
//        System.out.println(generateToText());
    }

    public void testNoThisNoClosure() throws Exception {
        blackBoxFile("functions/nothisnoclosure.jet");
//        System.out.println(generateToText());
    }

    public void testAnyToString () throws InvocationTargetException, IllegalAccessException {
        loadText("fun foo(x: Any) = x.toString()");
//        System.out.println(generateToText());
        Method foo = generateFunction();
        assertEquals("something", foo.invoke(null, "something"));
        assertEquals("null", foo.invoke(null, new Object[]{null}));

    }

    public void testNullableAnyToString () throws InvocationTargetException, IllegalAccessException {
        loadText("fun foo(x: Any?) = x.toString()");
//        System.out.println(generateToText());
        Method foo = generateFunction();
        assertEquals("something", foo.invoke(null, "something"));
        assertEquals("null", foo.invoke(null, new Object[]{null}));

    }

    public void testNullableStringPlus () throws InvocationTargetException, IllegalAccessException {
        loadText("fun foo(x: String?, y: Any?) = x + y");
        String text = generateToText();
        assertTrue(text.contains(".stringPlus"));
//        System.out.println(text);
        Method foo = generateFunction();
        assertEquals("something239", foo.invoke(null, "something", 239));
        assertEquals("null239", foo.invoke(null, null, 239));
        assertEquals("239null", foo.invoke(null, "239", null));
        assertEquals("nullnull", foo.invoke(null, null, null));

    }

    public void testNonNullableStringPlus () throws InvocationTargetException, IllegalAccessException {
        loadText("fun foo(x: String, y: Any?) = x + y + 120");
        String text = generateToText();
        assertFalse(text.contains(".stringPlus"));
//        System.out.println(text);
        Method foo = generateFunction();
        assertEquals("something239120", foo.invoke(null, "something", 239));
        assertEquals("null239120", foo.invoke(null, null, 239));
        assertEquals("239null120", foo.invoke(null, "239", null));
        assertEquals("nullnull120", foo.invoke(null, null, null));

    }

    public void testAnyEqualsNullable () throws InvocationTargetException, IllegalAccessException {
        loadText("fun foo(x: Any?) = x.equals(\"lala\")");
//        System.out.println(generateToText());
        Method foo = generateFunction();
        assertTrue((Boolean) foo.invoke(null, "lala"));
        assertFalse((Boolean) foo.invoke(null, "mama"));
    }

    public void testAnyEquals () throws InvocationTargetException, IllegalAccessException {
        loadText("fun foo(x: Any) = x.equals(\"lala\")");
//        System.out.println(generateToText());
        Method foo = generateFunction();
        assertTrue((Boolean) foo.invoke(null, "lala"));
        assertFalse((Boolean) foo.invoke(null, "mama"));
    }

    public void testKt395 () {
        blackBoxFile("regressions/kt395.jet");
    }

    public void testFunction () throws InvocationTargetException, IllegalAccessException {
        blackBoxFile("functions/functionExpression.jet");
    }
}
