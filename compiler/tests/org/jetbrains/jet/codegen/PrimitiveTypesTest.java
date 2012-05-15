/*
 * Copyright 2010-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.codegen;

import org.jetbrains.jet.lang.resolve.java.CompilerSpecialMode;

import java.lang.reflect.Method;

/**
 * @author yole
 * @author alex.tkachman
 */
public class PrimitiveTypesTest extends CodegenTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createEnvironmentWithMockJdkAndIdeaAnnotations(CompilerSpecialMode.ALT_HEADERS);
    }

    public void testPlus() throws Exception {
        loadText("fun f(a: Int, b: Int): Int { return a + b }");

//        System.out.println(generateToText());

        final Method main = generateFunction();
        final int returnValue = (Integer) main.invoke(null, 37, 5);
        assertEquals(42, returnValue);
    }

    public void testGt() throws Exception {
        loadText("fun foo(f: Int): Boolean { if (f > 0) return true; return false; }");

//        System.out.println(generateToText());
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
        loadText("fun foo(a: Int, b: Int): Int = if (a != b) 1 else 0");
        final Method main = generateFunction();
        assertEquals(0, main.invoke(null, 5, 5));
        assertEquals(1, main.invoke(null, 5, 3));
    }

    public void testGE() throws Exception {
        loadText("fun foo(a: Int, b: Int): Int = if (a >= b) 1 else 0");
        final Method main = generateFunction();
        assertEquals(1, main.invoke(null, 5, 5));
        assertEquals(0, main.invoke(null, 3, 5));
    }

    public void testReturnCmp() throws Exception {
        loadText("fun foo(a: Int, b: Int): Boolean = a == b");
        final Method main = generateFunction();
        assertEquals(true, main.invoke(null, 1, 1));
        assertEquals(false, main.invoke(null, 1, 2));
    }

    public void testLong() throws Exception {
        loadText("fun foo(a: Long, b: Long): Long = a + b");
//        System.out.println(generateToText());
        final Method main = generateFunction();
        long arg = (long) Integer.MAX_VALUE;
        long expected = 2 * (long) Integer.MAX_VALUE;
        assertEquals(expected, main.invoke(null, arg, arg));
    }

    public void testLongCmp() throws Exception {
        loadText("fun foo(a: Long, b: Long): Long = if (a == b) 0xffffffff else 0xfffffffe");
//        System.out.println(generateToText());
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
        // '+' is not available for Chars
        binOpTest("fun foo(a: Char, b: Char): Int = a - b", 'D', (char) 3, (int) 'A');
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
        loadText("import java.lang.Double as jlDouble; fun foo(d: Double): String? = jlDouble.toString(d)");
        final Method main = generateFunction();
        assertEquals("1.0", main.invoke(null, 1.0));
    }

    public void testDoubleToInt() throws Exception {
        loadText("fun foo(a: Double): Int = a.toInt()");
//        System.out.println(generateToText());
        final Method main = generateFunction();
        assertEquals(1, main.invoke(null, 1.0));
    }

    public void testCastConstant() throws Exception {
        loadText("fun foo(): Double = 1.toDouble()");
        final Method main = generateFunction();
        assertEquals(1.0, main.invoke(null));
    }

    public void testCastOnStack() throws Exception {
        loadText("fun foo(): Double = System.currentTimeMillis().toDouble()");
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
        loadText("fun foo(a: Int): Int { var x = a; ++x; return x;}");
        final Method main = generateFunction();
        assertEquals(11, main.invoke(null, 10));
    }

    public void testPreIncrementValue() throws Exception {
        loadText("fun foo(a: Int): Int { var x = a; return ++x;}");
        final Method main = generateFunction();
        assertEquals(11, main.invoke(null, 10));
    }

    public void testPreDecrement() throws Exception {
        loadText("fun foo(var a: Int): Int { return --a;}");
        final Method main = generateFunction();
        assertEquals(9, main.invoke(null, 10));
    }

    public void testPreIncrementLong() throws Exception {
        loadText("fun foo(var a: Long): Long = ++a");
        final Method main = generateFunction();
        assertEquals(11L, main.invoke(null, 10L));
    }

    public void testPreIncrementFloat() throws Exception {
        loadText("fun foo(var a: Float): Float = ++a");
        final Method main = generateFunction();
        assertEquals(2.0f, main.invoke(null, 1.0f));
    }

    public void testPreIncrementDouble() throws Exception {
        loadText("fun foo(var a: Double): Double = ++a");
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
//        System.out.println(generateToText());
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
//        System.out.println(generateToText());
        final Method main = generateFunction();
        main.invoke(null);  // ensure no exception
    }

    private void binOpTest(final String text, final Object arg1, final Object arg2, final Object expected) throws Exception {
        loadText(text);
//        System.out.println(generateToText());
        final Method main = generateFunction();
        assertEquals(expected, main.invoke(null, arg1, arg2));
    }

    public void testKt242 () throws Exception {
        blackBoxFile("regressions/kt242.jet");
    }

    public void testKt239 () throws Exception {
        blackBoxFile("regressions/kt239.jet");
    }

    public void testKt243 () throws Exception {
        blackBoxFile("regressions/kt243.jet");
    }

    public void testKt248 () throws Exception {
        blackBoxFile("regressions/kt248.jet");
    }

    public void testKt446 () throws Exception {
        blackBoxFile("regressions/kt446.jet");
    }

    public void testKt518 () throws Exception {
        blackBoxFile("regressions/kt518.jet");
    }

    public void testKt711 () throws Exception {
        loadText("fun box() = if ((1 ?: 0) == 1) \"OK\" else \"fail\"");
        blackBox();
    }

    public void testSureNonnull () throws Exception {
        loadText("fun box() = 10.sure().toString()");
        assertFalse(generateToText().contains("IFNONNULL"));
    }

    public void testSureNullable () throws Exception {
        loadText("val a : Int? = 10; fun box() = a.sure().toString()");
        assertTrue(generateToText().contains("IFNONNULL"));
    }

    public void testSafeNonnull () throws Exception {
        loadText("fun box() = 10?.toString()");
        assertFalse(generateToText().contains("IFNULL"));
    }

    public void testSafeNullable () throws Exception {
        loadText("val a : Int? = 10; fun box() = a?.toString()");
        assertTrue(generateToText().contains("IFNULL"));
    }

    public void testKt737() throws Exception {
        loadText("fun box() = if(3.compareTo(2) != 1) \"fail\" else if(5.toByte().compareTo(10.toLong()) >= 0) \"fail\" else \"OK\"");
        assertEquals("OK", blackBox());
    }

    public void testKt665() throws Exception {
        loadText("fun f(x: Long, zzz: Long = 1): Long\n" +
                 "{\n" +
                 "    return if (x <= 1) zzz\n" +
                 "    else f(x-1, x*zzz)\n" +
                 "}\n" +
                 "\n" +
                 "fun box() : String\n" +
                 "{\n" +
                 "    val six: Long = 6;\n" +
                 "    System.out?.println(f(six))\n" +
                 "    return \"OK\"" +
                 "}");
        blackBox();
    }

    public void testKt752 () {
        blackBoxFile("regressions/kt752.jet");
    }

    public void testKt753 () {
        blackBoxFile("regressions/kt753.jet");
    }

    public void testKt684 () {
        blackBoxFile("regressions/kt684.jet");
    }

    public void testKt756 () {
        blackBoxFile("regressions/kt756.jet");
        System.out.println(generateToText());
    }

    public void testKt757 () {
        blackBoxFile("regressions/kt757.jet");
    }

    public void testKt877 () {
        blackBoxFile("regressions/kt877.jet");
    }

    public void testKt882 () {
//        blackBoxFile("regressions/kt882.jet");
    }

    public void testKt887 () {
        blackBoxFile("regressions/kt887.jet");
    }

    public void testKt821 () {
        blackBoxFile("regressions/kt821.kt");
    }

    public void testKt925 () {
        blackBoxFile("regressions/kt925.kt");
    }

    public void testKt765 () {
        blackBoxFile("regressions/kt765.kt");
    }

    public void testKt930 () {
        blackBoxFile("regressions/kt930.kt");
    }

    public void testKt944 () {
        blackBoxFile("regressions/kt944.kt");
    }

    public void testKt945 () {
        blackBoxFile("regressions/kt945.jet");
    }

    public void testKt935 () {
        blackBoxFile("regressions/kt935.kt");
    }

    public void testKt1055 () {
        blackBoxFile("regressions/kt1055.kt");
    }

    public void testKt1054 () {
        blackBoxFile("regressions/kt1054.kt");
    }

    public void testKt1093 () {
        blackBoxFile("regressions/kt1093.kt");
    }

    public void testKt1508 () {
        blackBoxFile("regressions/kt1508.kt");
    }
}
