package org.jetbrains.jet.codegen;

import org.jetbrains.jet.parsing.JetParsingTest;

import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * @author yole
 */
public class NamespaceGenTest extends CodegenTestCase {
    public void testPSVM() throws Exception {
        loadFile("PSVM.jet");
        final String text = generateToText();
        System.out.println(text);

        final Method main = generateFunction();
        Object[] args = new Object[] { new String[0] };
        main.invoke(null, args);
    }

    public void testReturnOne() throws Exception {
        loadText("fun f() : Int { return 42; }");
        final String text = generateToText();
        System.out.println(text);

        final Method main = generateFunction();
        final Object returnValue = main.invoke(null, new Object[0]);
        assertEquals(new Integer(42), returnValue);
    }

    public void testReturnA() throws Exception {
        loadText("fun foo(a : Int) = a");
        final String text = generateToText();
        System.out.println(text);

        final Method main = generateFunction();
        final Object returnValue = main.invoke(null, 50);
        assertEquals(new Integer(50), returnValue);
    }

    public void testLocalProperty() throws Exception {
        myFixture.configureByFile(JetParsingTest.getTestDataDir() + "/codegen/localProperty.jet");
        final String text = generateToText();
        System.out.println(text);

        final Method main = generateFunction();
        final Object returnValue = main.invoke(null, 76);
        assertEquals(new Integer(50), returnValue);
    }

    public void testCurrentTime() throws Exception {
        loadText("fun f() : Long { return System.currentTimeMillis(); }");
        System.out.println(generateToText());
        final Method main = generateFunction();
        final long returnValue = (Long) main.invoke(null);
        long currentTime = System.currentTimeMillis();
        assertTrue(Math.abs(returnValue - currentTime) <= 1L);
    }

    public void testIdentityHashCode() throws Exception {
        loadText("fun f(o: Any) : Int { return System.identityHashCode(o); }");
        System.out.println(generateToText());
        final Method main = generateFunction();
        Object o = new Object();
        final int returnValue = (Integer) main.invoke(null, o);
        assertEquals(returnValue, System.identityHashCode(o));
    }

    public void testSystemOut() throws Exception {
        loadFile("systemOut.jet");
        final Method main = generateFunction();
        final Object returnValue = main.invoke(null);
        assertEquals(returnValue, System.out);
    }

    public void testHelloWorld() throws Exception {
        loadFile("helloWorld.jet");

        System.out.println(generateToText());

        generateFunction();  // assert that it can be verified
    }

    public void testAssign() throws Exception {
        loadFile("assign.jet");

        System.out.println(generateToText());
        final Method main = generateFunction();
        assertEquals(2, main.invoke(null));
    }

    public void _testBoxedInt() throws Exception {
        loadText("fun foo(a: Int?): Int = if (a != null) a else 239");
        final Method main = generateFunction();
        assertEquals(610, main.invoke(null, 610));
        assertEquals(239, main.invoke(null, new Object[]{null}));
    }

    public void testIntBoxed() throws Exception {
        loadText("fun foo(s: String): Int? = Integer.getInteger(s, 239)");
        final Method main = generateFunction();
        assertEquals(239, main.invoke(null, "no.such.system.property"));
    }

    public void testBoxConstant() throws Exception {
        loadText("fun foo(): Int? = 239");
        final Method main = generateFunction();
        assertEquals(239, main.invoke(null));
    }

    public void testBoxVariable() throws Exception {
        loadText("fun foo(): Int? { var x = 239; return x; }");
        System.out.println(generateToText());
        final Method main = generateFunction();
        assertEquals(239, main.invoke(null));
    }

    public void testAugAssign() throws Exception {
        loadText("fun foo(a: Int): Int { var x = a; x += 5; return x; }");
        final Method main = generateFunction();
        assertEquals(10, main.invoke(null, 5));
    }

    public void testBooleanNot() throws Exception {
        loadText("fun foo(b: Boolean): Boolean = !b");
        final Method main = generateFunction();
        assertEquals(true, main.invoke(null, false));
        assertEquals(false, main.invoke(null, true));
    }

    public void testBooleanNotJump() throws Exception {
        loadText("fun foo(a: Int) : Int = if (!(a < 5)) a else 0");
        final Method main = generateFunction();
        assertEquals(6, main.invoke(null, 6));
        assertEquals(0, main.invoke(null, 4));
    }

    public void testAnd() throws Exception {
        loadText("fun foo(a : Int): Boolean = a > 0 && a/0 > 0");
        final Method main = generateFunction();
        assertEquals(false, main.invoke(null, 0));
        boolean hadException = false;
        try {
            main.invoke(null, 5);
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof ArithmeticException) {
                hadException = true;
            }
        }
        assertTrue(hadException);
    }

    public void testOr() throws Exception {
        loadText("fun foo(a : Int): Boolean = a > 0 || a/0 > 0");
        final Method main = generateFunction();
        assertEquals(true, main.invoke(null, 5));
        boolean hadException = false;
        try {
            main.invoke(null, 0);
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof ArithmeticException) {
                hadException = true;
            }
        }
        assertTrue(hadException);
    }

    public void testBottles2() throws Exception {
        loadFile("bottles2.jet");
        System.out.println(generateToText());
        final Method main = generateFunction();
        main.invoke(null);  // ensure no exception
    }

    public void testJavaConstructor() throws Exception {
        loadText("fun foo(): StringBuilder = new StringBuilder()");
        System.out.println(generateToText());
        final Method main = generateFunction();
        final Object result = main.invoke(null);
        assertTrue(result instanceof StringBuilder);
    }

    public void testJavaConstructorWithParameters() throws Exception {
        loadText("fun foo(): StringBuilder = new StringBuilder(\"beer\")");
        final Method main = generateFunction();
        final StringBuilder result = (StringBuilder) main.invoke(null);
        assertEquals("beer", result.toString());
    }

    public void testJavaEquals() throws Exception {
        loadText("fun foo(s1: String, s2: String) = s1 == s2");
        System.out.println(generateToText());
        final Method main = generateFunction();
        assertEquals(Boolean.TRUE, main.invoke(null, new String("jet"), new String("jet")));
        assertEquals(Boolean.FALSE, main.invoke(null, new String("jet"), new String("ceylon")));
    }

    public void testJavaNotEquals() throws Exception {
        loadText("fun foo(s1: String, s2: String) = s1 != s2");
        final Method main = generateFunction();
        assertEquals(Boolean.FALSE, main.invoke(null, new String("jet"), new String("jet")));
        assertEquals(Boolean.TRUE, main.invoke(null, new String("jet"), new String("ceylon")));
    }

    public void testJavaEqualsNull() throws Exception {
        loadText("fun foo(s1: String?, s2: String?) = s1 == s2");
        final Method main = generateFunction();
        assertEquals(Boolean.TRUE, main.invoke(null, null, null));
        assertEquals(Boolean.FALSE, main.invoke(null, "jet", null));
        assertEquals(Boolean.FALSE, main.invoke(null, null, "jet"));
    }

    public void testEqualsNullLiteral() throws Exception {
        loadText("fun foo(s: String?) = s == null");
        final Method main = generateFunction();
        assertEquals(Boolean.TRUE, main.invoke(null, new Object[] { null }));
        assertEquals(Boolean.FALSE, main.invoke(null, "jet"));
    }

    public void testTripleEq() throws Exception {
        loadText("fun foo(s1: String?, s2: String?) = s1 === s2");
        final Method main = generateFunction();
        String s1 = new String("jet");
        String s2 = new String("jet");
        assertEquals(Boolean.TRUE, main.invoke(null, s1, s1));
        assertEquals(Boolean.FALSE, main.invoke(null, s1, s2));
    }

    public void testTripleNotEq() throws Exception {
        loadText("fun foo(s1: String?, s2: String?) = s1 !== s2");
        final Method main = generateFunction();
        String s1 = new String("jet");
        String s2 = new String("jet");
        assertEquals(Boolean.FALSE, main.invoke(null, s1, s1));
        assertEquals(Boolean.TRUE, main.invoke(null, s1, s2));
    }

    public void testFunctionCall() throws Exception {
        loadFile("functionCall.jet");
        System.out.println(generateToText());
        final Method main = generateFunction("f");
        assertEquals("foo", main.invoke(null));
    }

    public void testStringPlus() throws Exception {
        loadText("fun foo(s1: String, s2: String) = s1 + s2");
        final Method main = generateFunction();
        assertEquals("jetLang", main.invoke(null, "jet", "Lang"));
    }

    public void testStringPlusChained() throws Exception {
        loadText("fun foo(s1: String, s2: String, s3: String) = s1 + s2 + s3");
        final String text = generateToText();
        final int firstStringBuilderCreation = text.indexOf("NEW java/lang/StringBuilder");
        assertEquals(-1, text.indexOf("NEW java/lang/StringBuilder", firstStringBuilderCreation+1));
        final Method main = generateFunction();
        assertEquals("jet Lang", main.invoke(null, "jet", " ", "Lang"));
    }

    public void testStringCompare() throws Exception {
        loadText("fun foo(s1: String, s2: String) = s1 < s2");
        System.out.println(generateToText());
        final Method main = generateFunction();
        assertEquals(Boolean.TRUE, main.invoke(null, "Ceylon", "Java"));
        assertEquals(Boolean.FALSE, main.invoke(null, "Jet", "Java"));
    }

    public void _testVarargs() throws Exception {
        loadText("fun foo() = java.util.Arrays.asList(\"IntelliJ\", \"IDEA\")");
        final Method main = generateFunction();
        ArrayList arrayList = (ArrayList) main.invoke(null);
    }

    public void testFieldRead() throws Exception {
        loadText("import java.awt.*; fun foo(c: GridBagConstraints) = c.gridx");
        System.out.println(generateToText());
        final Method main = generateFunction();
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 239;
        assertEquals(239, main.invoke(null, c));
    }

    public void testFieldWrite() throws Exception {
        loadText("import java.awt.*; fun foo(c: GridBagConstraints) { c.gridx = 239 }");
        final Method main = generateFunction();
        GridBagConstraints c = new GridBagConstraints();
        main.invoke(null, c);
        assertEquals(239, c.gridx);
    }
}
