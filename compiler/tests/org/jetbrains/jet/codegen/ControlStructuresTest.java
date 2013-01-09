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

import org.jetbrains.jet.ConfigurationKind;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public class ControlStructuresTest extends CodegenTestCase {
    @Override
    protected String getPrefix() {
        return "controlStructures";
    }

    public void testIf() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        loadFile();

//        System.out.println(generateToText());
        final Method main = generateFunction();
        assertEquals(15, main.invoke(null, true));
        assertEquals(20, main.invoke(null, false));
    }

    public void testSingleBranchIf() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
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

    /*
    public void testInRangeConditionsInWhen() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("controlStructures/inRangeConditionsInWhen.jet");
    }
    */

    private void factorialTest(final String name) throws IllegalAccessException, InvocationTargetException {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        loadFile(name);

//        System.out.println(generateToText());
        final Method main = generateFunction();
        assertEquals(6, main.invoke(null, 3));
        assertEquals(120, main.invoke(null, 5));
    }

    public void testContinue() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        loadFile();
//        System.out.println(generateToText());
        final Method main = generateFunction();
        assertEquals(3, main.invoke(null, 4));
        assertEquals(7, main.invoke(null, 5));
    }

    public void testIfNoElse() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        loadFile();
//        System.out.println(generateToText());
        final Method main = generateFunction();
        assertEquals(5, main.invoke(null, 5, true));
        assertEquals(10, main.invoke(null, 5, false));
    }

    public void testCondJumpOnStack() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        loadText("import java.lang.Boolean as jlBoolean; fun foo(a: String): Int = if (jlBoolean.parseBoolean(a)) 5 else 10");
        final Method main = generateFunction();
        assertEquals(5, main.invoke(null, "true"));
        assertEquals(10, main.invoke(null, "false"));
    }

    public void testFor() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_AND_ANNOTATIONS);
        loadFile();
//        System.out.println(generateToText());
        final Method main = generateFunction();
        List<String> args = Arrays.asList("IntelliJ", " ", "IDEA");
        assertEquals("IntelliJ IDEA", main.invoke(null, args));
    }

    public void testIfBlock() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_AND_ANNOTATIONS);
        loadFile();
//        System.out.println(generateToText());
        final Method main = generateFunction();
        List<String> args = Arrays.asList("IntelliJ", " ", "IDEA");
        assertEquals("TTT", main.invoke(null, args));
        args = Arrays.asList("JetBrains");
        assertEquals("F", main.invoke(null, args));
    }

    public void testForInArray() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        loadFile();
//        System.out.println(generateToText());
        final Method main = generateFunction();
        String[] args = new String[] { "IntelliJ", " ", "IDEA" };
        assertEquals("IntelliJ IDEA", main.invoke(null, new Object[] { args }));
    }

    public void testForInRange() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        loadText("fun foo(sb: StringBuilder) { for(x in 1..4) sb.append(x) }");
        final Method main = generateFunction();
        StringBuilder stringBuilder = new StringBuilder();
        main.invoke(null, stringBuilder);
        assertEquals("1234", stringBuilder.toString());
    }

    public void testThrowCheckedException() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
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
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        loadFile();
//        System.out.println(generateToText());
        final Method main = generateFunction();
        assertEquals("no message", main.invoke(null, "0"));
        assertEquals("For input string: \"a\"", main.invoke(null, "a"));
    }

    public void testTryFinally() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
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
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("controlStructures/forUserType.jet");
    }

    public void testForLoopMemberExtensionNext() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("controlStructures/forLoopMemberExtensionNext.kt");
    }

    public void testForLoopMemberExtensionHasNext() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("controlStructures/forLoopMemberExtensionHasNext.kt");
    }

    public void testForLoopMemberExtensionAll() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("controlStructures/forLoopMemberExtensionAll.kt");
    }

    public void testForIntArray() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("controlStructures/forIntArray.jet");
    }

    public void testForPrimitiveIntArray() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("controlStructures/forPrimitiveIntArray.jet");
    }

    public void testForNullableIntArray() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("controlStructures/forNullableIntArray.jet");
    }

    public void testForIntRange() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("controlStructures/forIntRange.jet");
    }

    public void testKt237() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt237.jet");
    }

    public void testCompareToZero() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        loadText("fun foo(a: Int, b: Int): Boolean = a == 0 && b != 0 && 0 == a && 0 != b");
        String text = generateToText();
        /*
         * Check that the we generate optimized byte-code!
         */
        assertTrue(text.contains("IFEQ"));
        assertTrue(text.contains("IFNE"));
        assertFalse(text.contains("IF_ICMPEQ"));
        assertFalse(text.contains("IF_ICMPNE"));
        final Method main = generateFunction();
        assertEquals(true, main.invoke(null, 0, 1));
        assertEquals(false, main.invoke(null, 1, 0));
    }

    public void testCompareBoxedIntegerToZero() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("controlStructures/compareBoxedIntegerToZero.kt");
    }

    public void testCompareToNull() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        loadText("fun foo(a: String?, b: String?): Boolean = a == null && b !== null && null == a && null !== b");
        String text = generateToText();
        assertTrue(!text.contains("java/lang/Object.equals"));
        //System.out.println(text);
        final Method main = generateFunction();
        assertEquals(true, main.invoke(null, null, "lala"));
        assertEquals(false, main.invoke(null, null, null));
    }

    public void testCompareToNonnullableEq() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        loadText("fun foo(a: String?, b: String): Boolean = a == b || b == a");
//        System.out.println(generateToText());
        final Method main = generateFunction();
        assertEquals(false, main.invoke(null, null, "lala"));
        assertEquals(true, main.invoke(null, "papa", "papa"));
    }

    public void testCompareToNonnullableNotEq() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        loadText("fun foo(a: String?, b: String): Boolean = a != b");
        String text = generateToText();
//        System.out.println(text);
        assertTrue(text.contains("IXOR"));
        final Method main = generateFunction();
        assertEquals(true, main.invoke(null, null, "lala"));
        assertEquals(false, main.invoke(null, "papa", "papa"));
    }

    public void testKt299() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt299.jet");
    }

    public void testKt416() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt416.jet");
//        System.out.println(generateToText());
    }

    public void testKt513() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt513.jet");
    }

    public void testKt434() throws Exception {
        createEnvironmentWithFullJdk();
        blackBoxFile("regressions/kt434.jet");
    }

    public void testKt769() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt769.jet");
//        System.out.println(generateToText());
    }

    public void testKt773() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt773.jet");
//        System.out.println(generateToText());
    }

    public void testKt772() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt772.jet");
//        System.out.println(generateToText());
    }

    public void testKt870() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt870.jet");
//        System.out.println(generateToText());
    }

    public void testKt958() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt958.jet");
//        System.out.println(generateToText());
    }

    public void testQuicksort() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
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
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.ALL);
        blackBoxFile("regressions/kt1076.kt");
    }

    public void testKt998() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt998.kt");
    }

    public void testContinueInFor() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_AND_ANNOTATIONS);
        blackBoxFile("controlStructures/continueInFor.kt");
    }

    public void testContinueToLabelInFor() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_AND_ANNOTATIONS);
        blackBoxFile("controlStructures/continueToLabelInFor.kt");
    }

    public void testKt628() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt628.kt");
    }

    public void testKt1441() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt1441.kt");
    }

    public void testKt2147() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt2147.kt");
    }

    public void testIfDummy() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt1899.kt");
    }

    public void testKt1742() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt1742.kt");
    }

    public void testKt2062() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt2062.kt");
    }

    public void testKt910() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt910.kt");
    }

    public void testKt1688() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt1688.kt");
    }

    public void testKt2423() {
        createEnvironmentWithFullJdk();
        blackBoxFile("regressions/kt2423.kt");
    }

    public void testKt2416() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt2416.kt");
    }

    public void testKt2291() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt2291.kt");
    }

    public void testKt2259() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt2259.kt");
    }

    public void testKt2577() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt2577.kt");
    }

    public void testTryCatchFinallyChain() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("controlStructures/tryCatchFinallyChain.kt");
    }

    public void testKt2597() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt2597.kt");
    }

    public void testKt2598() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt2598.kt");
    }

    public void testLongRange() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("controlStructures/longRange.jet");
    }

    public void testForInSmartCastedToArray() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("controlStructures/forInSmartCastedToArray.kt");
    }

    public void testConditionOfEmptyIf() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("controlStructures/conditionOfEmptyIf.kt");
    }

    public void testFinallyOnEmptyReturn() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("controlStructures/finallyOnEmptyReturn.kt");
    }

    public void testKt3087() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt3087.kt");
    }

    public void testKt3203_1() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt3203_1.kt");
    }

    public void testKt3203_2() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFile("regressions/kt3203_2.kt");
    }
}
