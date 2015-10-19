/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen;

import com.intellij.util.ArrayUtil;
import org.jetbrains.kotlin.test.ConfigurationKind;

import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PackageGenTest extends CodegenTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
    }

    public void testPSVM() throws Exception {
        loadText("fun main(args: Array<String>) { }");
        Method main = generateFunction();
        Object[] args = new Object[] {ArrayUtil.EMPTY_STRING_ARRAY};
        main.invoke(null, args);
    }

    public void testReturnOne() throws Exception {
        loadText("fun f() : Int { return 42; }");
        Method main = generateFunction();
        Object returnValue = main.invoke(null);
        assertEquals(new Integer(42), returnValue);
    }

    public void testReturnA() throws Exception {
        loadText("fun foo(a : Int) = a");
        Method main = generateFunction();
        Object returnValue = main.invoke(null, 50);
        assertEquals(new Integer(50), returnValue);
    }

    public void testCurrentTime() throws Exception {
        loadText("fun f() : Long { return System.currentTimeMillis(); }");
        Method main = generateFunction();
        long actual = (Long) main.invoke(null);
        long expected = System.currentTimeMillis();
        long diff = Math.abs(actual - expected);
        assertTrue(
                "Difference with current time: " + diff + " (this test is a bad one: it may fail even if the generated code is correct)",
                diff <= 100
        );
    }

    public void testIdentityHashCode() throws Exception {
        loadText("fun f(o: Any) : Int { return System.identityHashCode(o); }");
        Method main = generateFunction();
        Object o = new Object();
        int returnValue = (Integer) main.invoke(null, o);
        assertEquals(returnValue, System.identityHashCode(o));
    }

    public void testSystemOut() throws Exception {
        loadText("fun f() : Any? { return System.out; }");
        Method main = generateFunction();
        Object returnValue = main.invoke(null);
        assertEquals(returnValue, System.out);
    }

    public void testBoxedInt() throws Exception {
        loadText("fun foo(a: Int?) = if (a != null) a else 239");
        Method main = generateFunction();
        assertEquals(610, main.invoke(null, 610));
        assertEquals(239, main.invoke(null, new Object[]{null}));
    }

    public void testIntBoxed() throws Exception {
        loadText("fun foo(s: String): Int? = Integer.getInteger(s, 239)");
        Method main = generateFunction();
        assertEquals(239, main.invoke(null, "no.such.system.property"));
    }

    public void testBoxConstant() throws Exception {
        loadText("fun foo(): Int? = 239");
        Method main = generateFunction();
        assertEquals(239, main.invoke(null));
    }

    public void testBoxVariable() throws Exception {
        loadText("fun foo(): Int? { var x = 239; return x; }");
        Method main = generateFunction();
        assertEquals(239, main.invoke(null));
    }

    public void testAugAssign() throws Exception {
        loadText("fun foo(a: Int): Int { var x = a; x += 5; return x; }");
        Method main = generateFunction();
        assertEquals(10, main.invoke(null, 5));
    }

    public void testBooleanNot() throws Exception {
        loadText("fun foo(b: Boolean): Boolean = !b");
        Method main = generateFunction();
        assertEquals(true, main.invoke(null, false));
        assertEquals(false, main.invoke(null, true));
    }

    public void testBooleanNotJump() throws Exception {
        loadText("fun foo(a: Int) : Int = if (!(a < 5)) a else 0");
        Method main = generateFunction();
        assertEquals(6, main.invoke(null, 6));
        assertEquals(0, main.invoke(null, 4));
    }

    public void testAnd() throws Exception {
        loadText("fun foo(a : Int): Boolean = a > 0 && a/0 > 0");
        Method main = generateFunction();
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
        Method main = generateFunction();
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

    public void testJavaConstructor() throws Exception {
        loadText("fun foo(): StringBuilder = StringBuilder()");
        Method main = generateFunction();
        Object result = main.invoke(null);
        assertTrue(result instanceof StringBuilder);
    }

    public void testJavaConstructorWithParameters() throws Exception {
        loadText("fun foo(): StringBuilder = StringBuilder(\"beer\")");
        Method main = generateFunction();
        StringBuilder result = (StringBuilder) main.invoke(null);
        assertEquals("beer", result.toString());
    }

    public void testJavaEquals() throws Exception {
        loadText("fun foo(s1: String, s2: String) = s1 == s2");
        Method main = generateFunction();
        assertEquals(Boolean.TRUE, main.invoke(null, new String("kotlin"), new String("kotlin")));
        assertEquals(Boolean.FALSE, main.invoke(null, new String("kotlin"), new String("ceylon")));
    }

    public void testJavaNotEquals() throws Exception {
        loadText("fun foo(s1: String, s2: String) = s1 != s2");
        Method main = generateFunction();
        assertEquals(Boolean.FALSE, main.invoke(null, new String("kotlin"), new String("kotlin")));
        assertEquals(Boolean.TRUE, main.invoke(null, new String("kotlin"), new String("ceylon")));
    }

    public void testJavaEqualsNull() throws Exception {
        loadText("fun foo(s1: String?, s2: String?) = s1 == s2");
        Method main = generateFunction();
        assertEquals(Boolean.TRUE, main.invoke(null, null, null));
        assertEquals(Boolean.FALSE, main.invoke(null, "kotlin", null));
        assertEquals(Boolean.FALSE, main.invoke(null, null, "kotlin"));
    }

    public void testEqualsNullLiteral() throws Exception {
        loadText("fun foo(s: String?) = s == null");
        Method main = generateFunction();
        assertEquals(Boolean.TRUE, main.invoke(null, new Object[] { null }));
        assertEquals(Boolean.FALSE, main.invoke(null, "kotlin"));
    }

    public void testTripleEq() throws Exception {
        loadText("fun foo(s1: String?, s2: String?) = s1 === s2");
        Method main = generateFunction();
        String s1 = new String("kotlin");
        String s2 = new String("kotlin");
        assertEquals(Boolean.TRUE, main.invoke(null, s1, s1));
        assertEquals(Boolean.FALSE, main.invoke(null, s1, s2));
    }

    public void testTripleNotEq() throws Exception {
        loadText("fun foo(s1: String?, s2: String?) = s1 !== s2");
        Method main = generateFunction();
        String s1 = new String("kotlin");
        String s2 = new String("kotlin");
        assertEquals(Boolean.FALSE, main.invoke(null, s1, s1));
        assertEquals(Boolean.TRUE, main.invoke(null, s1, s2));
    }

    public void testStringPlus() throws Exception {
        loadText("fun foo(s1: String, s2: String) = s1 + s2");
        Method main = generateFunction();
        assertEquals("kotlinLang", main.invoke(null, "kotlin", "Lang"));
    }

    public void testStringPlusChained() throws Exception {
        loadText("fun foo(s1: String, s2: String, s3: String) = s1 + s2 + s3");
        String text = generateToText();
        int firstStringBuilderCreation = text.indexOf("NEW java/lang/StringBuilder");
        assertEquals(-1, text.indexOf("NEW java/lang/StringBuilder", firstStringBuilderCreation + 1));
        Method main = generateFunction();
        assertEquals("kotlin Lang", main.invoke(null, "kotlin", " ", "Lang"));
    }

    public void testStringPlusEq() throws Exception {
        loadText("fun foo(s: String) : String { var result = s; result += s; return result; } ");
        Method main = generateFunction();
        assertEquals("JarJar", main.invoke(null, "Jar"));
    }

    public void testStringCompare() throws Exception {
        loadText("fun foo(s1: String, s2: String) = s1 < s2");
        Method main = generateFunction();
        assertEquals(Boolean.TRUE, main.invoke(null, "Ceylon", "Java"));
        assertEquals(Boolean.FALSE, main.invoke(null, "Kotlin", "Java"));
    }

    public void testElvis() throws Exception {
        loadText("fun foo(s: String?) = s ?: \"null\"");
        Method main = generateFunction();
        assertEquals("kotlin", main.invoke(null, "kotlin"));
        assertEquals("null", main.invoke(null, new Object[] { null }));
    }

    public void testElvisInt() throws Exception {
        loadText("fun foo(a: Int?): Int = a ?: 239");
        Method main = generateFunction();
        assertEquals(610, main.invoke(null, 610));
        assertEquals(239, main.invoke(null, new Object[]{null}));
    }

    public void testVarargs() throws Exception {
        loadText("fun foo() = java.util.Arrays.asList(\"IntelliJ\", \"IDEA\")");
        Method main = generateFunction();
        List<?> list = (List<?>) main.invoke(null);
        assertEquals(Arrays.asList("IntelliJ", "IDEA"), list);
    }

    public void testFieldRead() throws Exception {
        loadText("import java.awt.*; fun foo(c: GridBagConstraints) = c.gridx");
        Method main = generateFunction();
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 239;
        assertEquals(239, main.invoke(null, c));
    }

    public void testFieldWrite() throws Exception {
        loadText("import java.awt.*; fun foo(c: GridBagConstraints) { c.gridx = 239 }");
        Method main = generateFunction();
        GridBagConstraints c = new GridBagConstraints();
        main.invoke(null, c);
        assertEquals(239, c.gridx);
    }

    public void testFieldIncrement() throws Exception {
        loadText("import java.awt.*; fun foo(c: GridBagConstraints) { c.gridx++; return; }");
        Method main = generateFunction();
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 609;
        main.invoke(null, c);
        assertEquals(610, c.gridx);
    }

    public void testFieldAugAssign() throws Exception {
        loadText("import java.awt.*; fun foo(c: GridBagConstraints) { c.gridx *= 2; return; }");
        Method main = generateFunction();
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 305;
        main.invoke(null, c);
        assertEquals(610, c.gridx);
    }

    public void testIncrementAsLastOperation() throws Exception {
        loadText("fun foo() { var a = 0; a++; }");
        generateFunction();  // make sure we're not falling off end of code
    }

    public void testArrayRead() throws Exception {
        loadText("fun foo(c: Array<String>) = c[0]");
        Method main = generateFunction();
        assertEquals("main", main.invoke(null, new Object[]{new String[]{"main"}}));
    }

    public void testArrayWrite() throws Exception {
        loadText("fun foo(c: Array<String>) { c[0] = \"kotlin\"; }");
        Method main = generateFunction();
        String[] array = new String[] { null };
        main.invoke(null, new Object[] { array });
        assertEquals("kotlin", array[0]);
    }

    public void testArrayAugAssign() throws Exception {
        loadText("fun foo(c: Array<Int>) { c[0] *= 2 }");
        Method main = generateFunction();
        Integer[] data = new Integer[] { 5 };
        main.invoke(null, new Object[] { data });
        assertEquals(10, data[0].intValue());
    }

    public void testArrayAugAssignLong() throws Exception {
        loadText("fun foo(c: LongArray) { c[0] *= 2.toLong() }");
        Method main = generateFunction();
        long[] data = new long[] { 5 };
        main.invoke(null, new Object[] { data });
        assertEquals(10L, data[0]);
    }

    public void testArrayNewNullable() throws Exception {
        loadText("fun foo() = arrayOfNulls<Int>(4)");
        Method main = generateFunction();
        Integer[] result = (Integer[]) main.invoke(null);
        assertEquals(4, result.length);
    }
    public void testFloatArrayNew() throws Exception {
        loadText("fun foo() = FloatArray(4)");
        Method main = generateFunction();
        float[] result = (float[]) main.invoke(null);
        assertEquals(4, result.length);
    }

    public void testArraySize() throws Exception {
        loadText("fun foo(a: Array<Int>) = a.size");
        Method main = generateFunction();
        Object[] args = new Object[] { new Integer[4] };
        int result = (Integer) main.invoke(null, args);
        assertEquals(4, result);
    }

    public void testIntArraySize() throws Exception {
        loadText("fun foo(a: IntArray) = a.size");
        Method main = generateFunction();
        Object[] args = new Object[] { new int[4] };
        int result = (Integer) main.invoke(null, args);
        assertEquals(4, result);
    }

    public void testIntRange() throws Exception {
        loadText("fun foo() = 1..10");
        Method main = generateFunction();
        Object result = main.invoke(null);
        Method contains = result.getClass().getMethod("contains", Integer.TYPE);
        assertTrue((Boolean) contains.invoke(result, 1));
        assertTrue((Boolean) contains.invoke(result, 10));
        assertFalse((Boolean) contains.invoke(result, 11));
    }

    public void testSubstituteJavaMethodTypeParameters() throws Exception {
        loadText("import java.util.*; fun foo(l: ArrayList<Int>) { l.add(10) }");
        Method main = generateFunction();
        ArrayList<Integer> l = new ArrayList<Integer>();
        main.invoke(null, l);
        assertEquals(10, l.get(0).intValue());
    }

    public void testCallMethodDeclaredInSuperclass() throws Exception {
        loadText("fun foo(sb: StringBuilder) = sb.get(0)");
        Method main = generateFunction();
        StringBuilder sb = new StringBuilder("x");
        assertEquals('x', ((Character) main.invoke(null, sb)).charValue());
    }

    public void testPutBooleanAsVoid() throws Exception {
        loadText("class C(val x: Int) { init { x > 0 } } fun box() { val c = C(0) } ");
        Method main = generateFunction();
        main.invoke(null);  // must not fail
    }

    public void testJavaInterfaceMethod() throws Exception {
        loadText("import java.util.*; fun foo(l: ArrayList<String>) { l.add(\"foo\") }");
        Method main = generateFunction();
        ArrayList<String> list = new ArrayList<String>();
        main.invoke(null, list);
        assertEquals("foo", list.get(0));
    }

    public void testArrayAccessForArrayList() throws Exception {
        loadText("import java.util.*; fun foo(l: ArrayList<String>) { l[0] = \"Kotlin\" + l[0]; }");
        Method main = generateFunction();
        ArrayList<String> list = new ArrayList<String>();
        list.add("Language");
        main.invoke(null, list);
        assertEquals("KotlinLanguage", list.get(0));
    }

    public void testEscapeSequence() throws Exception {
        loadText("fun foo() = \"a\\nb\\$\"");
        Method main = generateFunction();
        assertEquals("a\nb$", main.invoke(null));
    }

    public void testStringTemplate() throws Exception {
        loadText("fun foo(a: String) = \"IntelliJ $a Rulezzz\"");
        Method main = generateFunction();
        assertEquals("IntelliJ IDEA Rulezzz", main.invoke(null, "IDEA"));
    }

    public void testExplicitCallOfBinaryOpIntrinsic() throws Exception {
        loadText("fun foo(a: Int) = a.plus(1)");
        Method main = generateFunction();
        assertEquals(2, ((Integer) main.invoke(null, 1)).intValue());
    }

    public void testExplicitCallOfUnaryMinusIntrinsic() throws Exception {
        loadText("fun foo(a: Int) = a.unaryMinus()");
        Method main = generateFunction();
        assertEquals(-1, ((Integer) main.invoke(null, 1)).intValue());
    }

    public void testExplicitCallOfBooleanNotIntrinsic() throws Exception {
        loadText("fun foo(a: Boolean) = a.not()");
        Method main = generateFunction();
        assertEquals(false, ((Boolean) main.invoke(null, true)).booleanValue());
    }

    public void testAppendArrayToString() throws Exception {
        loadText("fun foo(a: String, b: Array<String>) = a + b");
        Method main = generateFunction();
        String[] args = new String[] { "foo", "bar" };
        //noinspection ImplicitArrayToString
        assertEquals("s" + args.toString(), main.invoke(null, "s", args));
    }
}
