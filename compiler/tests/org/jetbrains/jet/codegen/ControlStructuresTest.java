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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.ConfigurationKind;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.jetbrains.jet.codegen.CodegenTestUtil.assertThrows;

public class ControlStructuresTest extends CodegenTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
    }

    @NotNull
    @Override
    protected String getPrefix() {
        return "controlStructures";
    }

    public void testIf() throws Exception {
        loadFile();
        final Method main = generateFunction();
        assertEquals(15, main.invoke(null, true));
        assertEquals(20, main.invoke(null, false));
    }

    public void testSingleBranchIf() throws Exception {
        loadFile();
        final Method main = generateFunction();
        assertEquals(15, main.invoke(null, true));
        assertEquals(20, main.invoke(null, false));
    }

    public void testWhile() throws Exception {
        factorialTest("controlStructures/while.kt");
    }

    public void testDoWhile() throws Exception {
        factorialTest("controlStructures/doWhile.kt");
    }

    public void testBreak() throws Exception {
        factorialTest("controlStructures/break.kt");
    }

    public void testInRangeConditionsInWhen() {
        blackBoxFile("controlStructures/inRangeConditionsInWhen.kt");
    }

    private void factorialTest(String name) throws Exception {
        loadFile(name);
        final Method main = generateFunction();
        assertEquals(6, main.invoke(null, 3));
        assertEquals(120, main.invoke(null, 5));
    }

    public void testContinue() throws Exception {
        loadFile();
        final Method main = generateFunction();
        assertEquals(3, main.invoke(null, 4));
        assertEquals(7, main.invoke(null, 5));
    }

    public void testIfNoElse() throws Exception {
        loadFile();
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
        final Method main = generateFunction();
        List<String> args = Arrays.asList("IntelliJ", " ", "IDEA");
        assertEquals("IntelliJ IDEA", main.invoke(null, args));
    }

    public void testIfBlock() throws Exception {
        loadFile();
        final Method main = generateFunction();
        List<String> args = Arrays.asList("IntelliJ", " ", "IDEA");
        assertEquals("TTT", main.invoke(null, args));
        args = Arrays.asList("JetBrains");
        assertEquals("F", main.invoke(null, args));
    }

    public void testForInArray() throws Exception {
        loadFile();
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
        assertThrows(main, Exception.class, null);
    }

    public void testTryCatch() throws Exception {
        loadFile();
        final Method main = generateFunction();
        assertEquals("no message", main.invoke(null, "0"));
        assertEquals("For input string: \"a\"", main.invoke(null, "a"));
    }

    public void testTryFinally() throws Exception {
        loadFile();
        final Method main = generateFunction();
        StringBuilder sb = new StringBuilder();
        main.invoke(null, sb, "9");
        assertEquals("foo9bar", sb.toString());
        sb = new StringBuilder();
        assertThrows(main, NumberFormatException.class, null, sb, "x");
        assertEquals("foobar", sb.toString());
    }

    public void testForUserType() {
        blackBoxFile("controlStructures/forUserType.kt");
    }

    public void testForLoopMemberExtensionNext() {
        blackBoxFile("controlStructures/forLoopMemberExtensionNext.kt");
    }

    public void testForLoopMemberExtensionHasNext() {
        blackBoxFile("controlStructures/forLoopMemberExtensionHasNext.kt");
    }

    public void testForLoopMemberExtensionAll() {
        blackBoxFile("controlStructures/forLoopMemberExtensionAll.kt");
    }

    public void testForIntArray() {
        blackBoxFile("controlStructures/forIntArray.kt");
    }

    public void testForPrimitiveIntArray() {
        blackBoxFile("controlStructures/forPrimitiveIntArray.kt");
    }

    public void testForNullableIntArray() {
        blackBoxFile("controlStructures/forNullableIntArray.kt");
    }

    public void testForIntRange() {
        blackBoxFile("controlStructures/forIntRange.kt");
    }

    public void testKt237() {
        blackBoxFile("regressions/kt237.kt");
    }

    public void testCompareToZero() throws Exception {
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
        blackBoxFile("controlStructures/compareBoxedIntegerToZero.kt");
    }

    public void testCompareToNull() throws Exception {
        loadText("fun foo(a: String?, b: String?): Boolean = a == null && b !== null && null == a && null !== b");
        String text = generateToText();
        assertTrue(!text.contains("java/lang/Object.equals"));
        final Method main = generateFunction();
        assertEquals(true, main.invoke(null, null, "lala"));
        assertEquals(false, main.invoke(null, null, null));
    }

    public void testCompareToNonnullableEq() throws Exception {
        loadText("fun foo(a: String?, b: String): Boolean = a == b || b == a");
        final Method main = generateFunction();
        assertEquals(false, main.invoke(null, null, "lala"));
        assertEquals(true, main.invoke(null, "papa", "papa"));
    }

    public void testCompareToNonnullableNotEq() throws Exception {
        loadText("fun foo(a: String?, b: String): Boolean = a != b");
        String text = generateToText();
        assertTrue(text.contains("IXOR"));
        final Method main = generateFunction();
        assertEquals(true, main.invoke(null, null, "lala"));
        assertEquals(false, main.invoke(null, "papa", "papa"));
    }

    public void testKt299() {
        blackBoxFile("regressions/kt299.kt");
    }

    public void testKt416() {
        blackBoxFile("regressions/kt416.kt");
    }

    public void testKt513() {
        blackBoxFile("regressions/kt513.kt");
    }

    public void testKt769() {
        blackBoxFile("regressions/kt769.kt");
    }

    public void testKt773() {
        blackBoxFile("regressions/kt773.kt");
    }

    public void testKt772() {
        blackBoxFile("regressions/kt772.kt");
    }

    public void testKt870() {
        blackBoxFile("regressions/kt870.kt");
    }

    public void testKt958() {
        blackBoxFile("regressions/kt958.kt");
    }

    public void testQuicksort() {
        blackBoxFile("controlStructures/quicksort.kt");
    }

    public void testKt998() {
        blackBoxFile("regressions/kt998.kt");
    }

    public void testContinueInFor() {
        blackBoxFile("controlStructures/continueInFor.kt");
    }

    public void testContinueToLabelInFor() {
        blackBoxFile("controlStructures/continueToLabelInFor.kt");
    }

    public void testKt628() {
        blackBoxFile("regressions/kt628.kt");
    }

    public void testKt1441() {
        blackBoxFile("regressions/kt1441.kt");
    }

    public void testKt2147() {
        blackBoxFile("regressions/kt2147.kt");
    }

    public void testIfDummy() {
        blackBoxFile("regressions/kt1899.kt");
    }

    public void testKt1742() {
        blackBoxFile("regressions/kt1742.kt");
    }

    public void testKt2062() {
        blackBoxFile("regressions/kt2062.kt");
    }

    public void testKt910() {
        blackBoxFile("regressions/kt910.kt");
    }

    public void testKt1688() {
        blackBoxFile("regressions/kt1688.kt");
    }

    public void testKt2416() {
        blackBoxFile("regressions/kt2416.kt");
    }

    public void testKt2291() {
        blackBoxFile("regressions/kt2291.kt");
    }

    public void testKt2259() {
        blackBoxFile("regressions/kt2259.kt");
    }

    public void testKt2577() {
        blackBoxFile("regressions/kt2577.kt");
    }

    public void testTryCatchFinallyChain() {
        blackBoxFile("controlStructures/tryCatchFinallyChain.kt");
    }

    public void testKt2597() {
        blackBoxFile("regressions/kt2597.kt");
    }

    public void testKt2598() {
        blackBoxFile("regressions/kt2598.kt");
    }

    public void testLongRange() {
        blackBoxFile("controlStructures/longRange.kt");
    }

    public void testForInSmartCastedToArray() {
        blackBoxFile("controlStructures/forInSmartCastedToArray.kt");
    }

    public void testConditionOfEmptyIf() {
        blackBoxFile("controlStructures/conditionOfEmptyIf.kt");
    }

    public void testFinallyOnEmptyReturn() {
        blackBoxFile("controlStructures/finallyOnEmptyReturn.kt");
    }

    public void testKt3087() {
        blackBoxFile("regressions/kt3087.kt");
    }

    public void testKt3203_1() {
        blackBoxFile("regressions/kt3203_1.kt");
    }

    public void testKt3203_2() {
        blackBoxFile("regressions/kt3203_2.kt");
    }

    public void testKt3273() {
        blackBoxFile("regressions/kt3273.kt");
    }
}
