package org.jetbrains.jet.codegen;

import java.lang.reflect.Method;

/**
 * @author yole
 */
public class PrimitiveTypesTest extends CodegenTestCase {
    public void testPlus() throws Exception {
        loadText("fun f(a: Int, b: Int): Int { return a + b }");

        System.out.println(generateToText());

        final Method main = generateFunction();
        final int returnValue = (Integer) main.invoke(null, 37, 5);
        assertEquals(42, returnValue);
    }

    public void testGt() throws Exception {
        loadText("fun foo(f: Int): Boolean { if (f > 0) return true; return false; }");

        System.out.println(generateToText());
        final Method main = generateFunction();
        assertEquals(true, main.invoke(null, 1));
        assertEquals(false, main.invoke(null, 0));
    }

    public void testDiv() throws Exception {
        binOpTest("fun foo(a: Int, b: Int): Int = a / b", 12, 3, 4);
    }

    public void testMod() throws Exception {
        binOpTest("fun foo(a: Int, b: Int): Int = a % b", 14, 3, 2);
    }

    public void testNE() throws Exception {
        final String code = "fun foo(a: Int, b: Int): Int = if (a != b) 1 else 0";
        binOpTest(code, 5, 5, 0);
        binOpTest(code, 5, 3, 1);
    }

    public void testGE() throws Exception {
        final String code = "fun foo(a: Int, b: Int): Int = if (a >= b) 1 else 0";
        binOpTest(code, 5, 5, 1);
        binOpTest(code, 3, 5, 0);
    }

    public void testReturnCmp() throws Exception {
        loadText("fun foo(a: Int, b: Int): Boolean = a == b");
        final Method main = generateFunction();
        assertEquals(true, main.invoke(null, 1, 1));
        assertEquals(false, main.invoke(null, 1, 2));
    }

    public void testLong() throws Exception {
        loadText("fun foo(a: Long, b: Long): Long = a + b");
        System.out.println(generateToText());
        final Method main = generateFunction();
        long arg = (long) Integer.MAX_VALUE;
        long expected = 2 * (long) Integer.MAX_VALUE;
        assertEquals(expected, main.invoke(null, arg, arg));
    }

    public void testLongCmp() throws Exception {
        loadText("fun foo(a: Long, b: Long): Long = if (a == b) 0xffffffff else 0xfffffffe");
        System.out.println(generateToText());
        final Method main = generateFunction();
        assertEquals(0xffffffffL, main.invoke(null, 1, 1));
        assertEquals(0xfffffffeL, main.invoke(null, 1, 0));
    }

    public void testShort() throws Exception {
        binOpTest("fun foo(a: Short, b: Short): Int = a + b",
                Short.valueOf((short) 32767), Short.valueOf((short) 32767), 65534);
    }

    public void testShortCmp() throws Exception {
        binOpTest("fun foo(a: Short, b: Short): Boolean = a == b",
                Short.valueOf((short) 32767), Short.valueOf((short) 32767), true);
    }

    public void testByte() throws Exception {
        binOpTest("fun foo(a: Byte, b: Byte): Int = a + b",
                Byte.valueOf((byte) 127), Byte.valueOf((byte) 127), 254);
    }

    public void testByteCmp() throws Exception {
        binOpTest("fun foo(a: Byte, b: Byte): Int = if (a == b) 1 else 0",
                Byte.valueOf((byte) 127), Byte.valueOf((byte) 127), 1);
    }

    public void testByteLess() throws Exception {
        binOpTest("fun foo(a: Byte, b: Byte): Boolean = a < b",
                Byte.valueOf((byte) 126), Byte.valueOf((byte) 127), true);
    }

    public void testBooleanConstant() throws Exception {
        loadText("fun foo(): Boolean = true");
        final Method main = generateFunction();
        assertEquals(true, main.invoke(null));
    }

    public void testChar() throws Exception {
        binOpTest("fun foo(a: Char, b: Char): Int = a + b", 'A', (char) 3, (int) 'D');
    }

    public void testFloat() throws Exception {
        binOpTest("fun foo(a: Float, b: Float): Float = a + b", 1.0f, 2.0f, 3.0f);
    }

    public void testFloatCmp() throws Exception {
        binOpTest("fun foo(a: Float, b: Float): Boolean = a == b", 1.0f, 1.0f, true);
    }

    public void testDouble() throws Exception {
        binOpTest("fun foo(a: Double, b: Double): Double = a + b", 1.0, 2.0, 3.0);
    }

    public void testDoubleCmp() throws Exception {
        binOpTest("fun foo(a: Double, b: Double): Boolean = a == b", 1.0, 2.0, false);
    }

    public void testDoubleToJava() throws Exception {
        loadText("fun foo(d: Double): String? = Double.toString(d)");
        final Method main = generateFunction();
        assertEquals("1.0", main.invoke(null, 1.0));
    }

    public void testDoubleToInt() throws Exception {
        loadText("fun foo(a: Double): Int = a.int");
        System.out.println(generateToText());
        final Method main = generateFunction();
        assertEquals(1, main.invoke(null, 1.0));
    }

    public void testCastConstant() throws Exception {
        loadText("fun foo(): Double = 1.dbl");
        final Method main = generateFunction();
        assertEquals(1.0, main.invoke(null));
    }

    public void testCastOnStack() throws Exception {
        loadText("fun foo(): Double = System.currentTimeMillis().dbl");
        final Method main = generateFunction();
        double currentTimeMillis = (double) System.currentTimeMillis();
        double result = (Double) main.invoke(null);
        double delta = Math.abs(currentTimeMillis - result);
        assertTrue(delta <= 1.0);
    }

    public void testNeg() throws Exception {
        loadText("fun foo(a: Int): Int = -a");
        final Method main = generateFunction();
        assertEquals(-10, main.invoke(null, 10));
    }

    public void testPreIncrement() throws Exception {
        loadText("fun foo(a: Int): Int { var x = a; ++x; return x;");
        final Method main = generateFunction();
        assertEquals(11, main.invoke(null, 10));
    }

    public void testPreIncrementValue() throws Exception {
        loadText("fun foo(a: Int): Int { var x = a; return ++x;");
        final Method main = generateFunction();
        assertEquals(11, main.invoke(null, 10));
    }

    public void testPreDecrement() throws Exception {
        loadText("fun foo(a: Int): Int { return --a;");
        final Method main = generateFunction();
        assertEquals(9, main.invoke(null, 10));
    }

    public void testPreIncrementLong() throws Exception {
        loadText("fun foo(a: Long): Long = ++a");
        final Method main = generateFunction();
        assertEquals(11L, main.invoke(null, 10L));
    }

    public void testPreIncrementFloat() throws Exception {
        loadText("fun foo(a: Float): Float = ++a");
        final Method main = generateFunction();
        assertEquals(2.0f, main.invoke(null, 1.0f));
    }

    public void testPreIncrementDouble() throws Exception {
        loadText("fun foo(a: Double): Double = ++a");
        final Method main = generateFunction();
        assertEquals(2.0, main.invoke(null, 1.0));
    }

    public void testShl() throws Exception {
        binOpTest("fun foo(a: Int, b: Int): Int = a shl b", 1, 3, 8);
    }

    public void testShr() throws Exception {
        binOpTest("fun foo(a: Int, b: Int): Int = a shr b", 8, 3, 1);
    }

    public void testBitAnd() throws Exception {
        binOpTest("fun foo(a: Int, b: Int): Int = a and b", 0x77, 0x1f, 0x17);
    }

    public void testBitOr() throws Exception {
        binOpTest("fun foo(a: Int, b: Int): Int = a or b", 0x77, 0x1f, 0x7f);
    }

    public void testBitXor() throws Exception {
        binOpTest("fun foo(a: Int, b: Int): Int = a xor b", 0x70, 0x1f, 0x6f);
    }

    public void testBitInv() throws Exception {
        loadText("fun foo(a: Int): Int = a.inv()");
        System.out.println(generateToText());
        final Method main = generateFunction();
        assertEquals(0xffff0000, main.invoke(null, 0x0000ffff));
    }

    public void testMixedTypes() throws Exception {
        binOpTest("fun foo(a: Int, b: Long): Long = a + b", 1, 2L, 3L);
    }

    public void testMixedTypes2() throws Exception {
        binOpTest("fun foo(a: Double, b: Int): Double = a + b", 1.0, 2, 3.0);
    }

    public void testPostIncrementTypeInferenceFail() throws Exception {
        loadText("fun foo(a: Int): Int { var x = a; var y = x++; if (y+1 != x) return -1; return x; }");
        final Method main = generateFunction();
        assertEquals(6, main.invoke(null, 5));
    }

    public void testPostIncrement() throws Exception {
        loadText("fun foo(a: Int): Int { var x = a; var y = x++; return x*y; }");
        final Method main = generateFunction();
        assertEquals(6, main.invoke(null, 2));
    }

    public void testPostIncrementLong() throws Exception {
        loadText("fun foo(a: Long): Long { var x = a; var y = x++; return x*y; }");
        final Method main = generateFunction();
        assertEquals(6L, main.invoke(null, 2L));
    }

    public void testDecrementAsStatement() throws Exception {
        loadFile("bottles.jet");
        System.out.println(generateToText());
        final Method main = generateFunction();
        main.invoke(null);  // ensure no exception
    }

    private void binOpTest(final String text, final Object arg1, final Object arg2, final Object expected) throws Exception {
        loadText(text);
        System.out.println(generateToText());
        final Method main = generateFunction();
        assertEquals(expected, main.invoke(null, arg1, arg2));
    }


}
