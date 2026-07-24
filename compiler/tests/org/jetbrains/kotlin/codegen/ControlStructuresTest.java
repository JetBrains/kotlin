/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.test.ConfigurationKind;
import org.jetbrains.kotlin.test.FirParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

public class ControlStructuresTest extends CodegenTestCase {

    @Override
    public @NotNull FirParser getFirParser() {
        return FirParser.LightTree;
    }

    @BeforeEach
    protected void setUp() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
    }

    @Test
    public void testCondJumpOnStack() throws Exception {
        loadText("import java.lang.Boolean as jlBoolean; fun foo(a: String): Int = if (jlBoolean.parseBoolean(a)) 5 else 10");
        Method main = generateFunction();
        assertEquals(5, main.invoke(null, "true"));
        assertEquals(10, main.invoke(null, "false"));
    }

    @Test
    public void testForInRange() throws Exception {
        loadText("fun foo(sb: StringBuilder) { for(x in 1..4) sb.append(x) }");
        Method main = generateFunction();
        StringBuilder stringBuilder = new StringBuilder();
        main.invoke(null, stringBuilder);
        assertEquals("1234", stringBuilder.toString());
    }

    @Test
    public void testThrowCheckedException() throws Exception {
        loadText("fun foo() { throw Exception(); }");
        Method main = generateFunction();
        CodegenTestUtil.assertThrows(main, Exception.class, null);
    }

    @Test
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
        Method main = generateFunction();
        assertEquals(true, main.invoke(null, 0, 1));
        assertEquals(false, main.invoke(null, 1, 0));
    }

    @Test
    public void testCompareToNull() throws Exception {
        loadText("fun foo(a: String?, b: String?): Boolean = a == null && b !== null && null == a && null !== b");
        String text = generateToText();
        assertFalse(text.contains("java/lang/Object.equals"));
        Method main = generateFunction();
        assertEquals(true, main.invoke(null, null, "lala"));
        assertEquals(false, main.invoke(null, null, null));
    }

    @Test
    public void testCompareToNonnullableEq() throws Exception {
        loadText("fun foo(a: String?, b: String): Boolean = a == b || b == a");
        Method main = generateFunction();
        assertEquals(false, main.invoke(null, null, "lala"));
        assertEquals(true, main.invoke(null, "papa", "papa"));
    }
}
