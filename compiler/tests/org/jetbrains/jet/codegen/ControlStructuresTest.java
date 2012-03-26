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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * @author yole
 */
public class ControlStructuresTest extends CodegenTestCase {
    @Override
    protected String getPrefix() {
        return "controlStructures";
    }

    public void testIf() throws Exception {
        loadFile();

//        System.out.println(generateToText());
        final Method main = generateFunction();
        assertEquals(15, main.invoke(null, true));
        assertEquals(20, main.invoke(null, false));
    }

    public void testSingleBranchIf() throws Exception {
        loadFile();

//        System.out.println(generateToText());
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

//        System.out.println(generateToText());
        final Method main = generateFunction();
        assertEquals(6, main.invoke(null, 3));
        assertEquals(120, main.invoke(null, 5));
    }

    public void testContinue() throws Exception {
        loadFile();
//        System.out.println(generateToText());
        final Method main = generateFunction();
        assertEquals(3, main.invoke(null, 4));
        assertEquals(7, main.invoke(null, 5));
    }

    public void testIfNoElse() throws Exception {
        loadFile();
//        System.out.println(generateToText());
        final Method main = generateFunction();
        assertEquals(5, main.invoke(null, 5, true));
        assertEquals(10, main.invoke(null, 5, false));
    }

    public void testCondJumpOnStack() throws Exception {
        loadText("import java.lang.Boolean as jlBoolean; fun foo(a: String): Int = if (jlBoolean.parseBoolean(a)) 5 else 10");
        final Method main = generateFunction();
        assertEquals(5, main.invoke(null, "true"));
        assertEquals(10, main.invoke(null, "false"));
    }

    public void testFor() throws Exception {
        loadFile();
//        System.out.println(generateToText());
        final Method main = generateFunction();
        List<String> args = Arrays.asList("IntelliJ", " ", "IDEA");
        assertEquals("IntelliJ IDEA", main.invoke(null, args));
    }

    public void testIfBlock() throws Exception {
        loadFile();
//        System.out.println(generateToText());
        final Method main = generateFunction();
        List<String> args = Arrays.asList("IntelliJ", " ", "IDEA");
        assertEquals("TTT", main.invoke(null, args));
        args = Arrays.asList("JetBrains");
        assertEquals("F", main.invoke(null, args));
    }

    public void testForInArray() throws Exception {
        loadFile();
//        System.out.println(generateToText());
        final Method main = generateFunction();
        String[] args = new String[] { "IntelliJ", " ", "IDEA" };
        assertEquals("IntelliJ IDEA", main.invoke(null, new Object[] { args }));
    }

    public void testForInRange() throws Exception {
        loadText("fun foo(sb: StringBuilder) { for(x in 1..4) sb.append(x) }");
        final Method main = generateFunction();
        StringBuilder stringBuilder = new StringBuilder();
        main.invoke(null, stringBuilder);
        assertEquals("1234", stringBuilder.toString());
    }

    public void testThrowCheckedException() throws Exception {
        loadText("fun foo() { throw Exception(); }");
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
        loadFile();
//        System.out.println(generateToText());
        final Method main = generateFunction();
        assertEquals("no message", main.invoke(null, "0"));
        assertEquals("For input string: \"a\"", main.invoke(null, "a"));
    }

    public void testTryFinally() throws Exception {
        loadFile();
//        System.out.println(generateToText());
        final Method main = generateFunction();
        StringBuilder sb = new StringBuilder();
        main.invoke(null, sb, "9");
        assertEquals("foo9bar", sb.toString());
        sb = new StringBuilder();
        boolean caught = false;
        try {
            main.invoke(null, sb, "x");
        }
        catch(InvocationTargetException e) {
            caught = e.getTargetException() instanceof NumberFormatException;
        }
        assertTrue(caught);
        assertEquals("foobar", sb.toString());
    }

    public void testForUserType() throws Exception {
        blackBoxFile("controlStructures/forUserType.jet");
    }

    public void testForIntArray() throws Exception {
        blackBoxFile("controlStructures/forIntArray.jet");
    }

    public void testForPrimitiveIntArray() throws Exception {
        blackBoxFile("controlStructures/forPrimitiveIntArray.jet");
    }

    public void testForNullableIntArray() throws Exception {
        blackBoxFile("controlStructures/forNullableIntArray.jet");
    }

    public void testForIntRange() {
        blackBoxFile("controlStructures/forIntRange.jet");
    }

    public void testKt237() throws Exception {
        blackBoxFile("regressions/kt237.jet");
    }

    public void testCompareToNull() throws Exception {
        loadText("fun foo(a: String?, b: String?): Boolean = a == null && b !== null && null == a && null !== b");
        String text = generateToText();
        assertTrue(!text.contains("java/lang/Object.equals"));
        System.out.println(text);
        final Method main = generateFunction();
        assertEquals(true, main.invoke(null, null, "lala"));
        assertEquals(false, main.invoke(null, null, null));
    }

    public void testCompareToNonnullableEq() throws Exception {
        loadText("fun foo(a: String?, b: String): Boolean = a == b || b == a");
//        System.out.println(generateToText());
        final Method main = generateFunction();
        assertEquals(false, main.invoke(null, null, "lala"));
        assertEquals(true, main.invoke(null, "papa", "papa"));
    }

    public void testCompareToNonnullableNotEq() throws Exception {
        loadText("fun foo(a: String?, b: String): Boolean = a != b");
        String text = generateToText();
//        System.out.println(text);
        assertTrue(text.contains("IXOR"));
        final Method main = generateFunction();
        assertEquals(true, main.invoke(null, null, "lala"));
        assertEquals(false, main.invoke(null, "papa", "papa"));
    }

    public void testKt299() throws Exception {
        blackBoxFile("regressions/kt299.jet");
    }

    public void testKt416() throws Exception {
        blackBoxFile("regressions/kt416.jet");
//        System.out.println(generateToText());
    }

    public void testKt513() throws Exception {
        blackBoxFile("regressions/kt513.jet");
    }

    public void testKt434() throws Exception {
        createEnvironmentWithFullJdk();
        blackBoxFile("regressions/kt434.jet");
    }

    public void testKt769() throws Exception {
        blackBoxFile("regressions/kt769.jet");
//        System.out.println(generateToText());
    }

    public void testKt773() throws Exception {
        blackBoxFile("regressions/kt773.jet");
//        System.out.println(generateToText());
    }

    public void testKt772() throws Exception {
        blackBoxFile("regressions/kt772.jet");
//        System.out.println(generateToText());
    }

    public void testKt870() throws Exception {
        blackBoxFile("regressions/kt870.jet");
//        System.out.println(generateToText());
    }

    public void testKt958() throws Exception {
        blackBoxFile("regressions/kt958.jet");
//        System.out.println(generateToText());
    }

    public void testQuicksort() throws Exception {
        blackBoxFile("controlStructures/quicksort.jet");
//        System.out.println(generateToText());
    }

    public void testSynchronized() throws Exception {
        createEnvironmentWithFullJdk();
        blackBoxFile("controlStructures/sync.jet");
//        System.out.println(generateToText());
    }

    public void testIfInWhile() throws Exception {
        createEnvironmentWithFullJdk();
        blackBoxFile("controlStructures/ifInWhile.jet");
//        System.out.println(generateToText());
    }

    public void testKt1076() throws Exception {
        blackBoxFile("regressions/kt1076.kt");
    }

    public void testKt998() throws Exception {
        blackBoxFile("regressions/kt998.kt");
    }

    public void testKt628() throws Exception {
        blackBoxFile("regressions/kt628.kt");
    }
}
