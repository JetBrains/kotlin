/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import org.jetbrains.kotlin.test.ConfigurationKind;

import java.lang.reflect.Method;

import static org.jetbrains.kotlin.codegen.CodegenTestUtil.assertThrows;

public class ControlStructuresTest extends CodegenTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
    }

    public void testCondJumpOnStack() throws Exception {
        loadText("import java.lang.Boolean as jlBoolean; fun foo(a: String): Int = if (jlBoolean.parseBoolean(a)) 5 else 10");
        Method main = generateFunction();
        assertEquals(5, main.invoke(null, "true"));
        assertEquals(10, main.invoke(null, "false"));
    }

    public void testForInRange() throws Exception {
        loadText("fun foo(sb: StringBuilder) { for(x in 1..4) sb.append(x) }");
        Method main = generateFunction();
        StringBuilder stringBuilder = new StringBuilder();
        main.invoke(null, stringBuilder);
        assertEquals("1234", stringBuilder.toString());
    }

    public void testThrowCheckedException() throws Exception {
        loadText("fun foo() { throw Exception(); }");
        Method main = generateFunction();
        assertThrows(main, Exception.class, null);
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
        Method main = generateFunction();
        assertEquals(true, main.invoke(null, 0, 1));
        assertEquals(false, main.invoke(null, 1, 0));
    }

    public void testCompareToNull() throws Exception {
        loadText("fun foo(a: String?, b: String?): Boolean = a == null && b !== null && null == a && null !== b");
        String text = generateToText();
        assertTrue(!text.contains("java/lang/Object.equals"));
        Method main = generateFunction();
        assertEquals(true, main.invoke(null, null, "lala"));
        assertEquals(false, main.invoke(null, null, null));
    }

    public void testCompareToNonnullableEq() throws Exception {
        loadText("fun foo(a: String?, b: String): Boolean = a == b || b == a");
        Method main = generateFunction();
        assertEquals(false, main.invoke(null, null, "lala"));
        assertEquals(true, main.invoke(null, "papa", "papa"));
    }

    public void testCompareToNonnullableNotEq() throws Exception {
        loadText("fun foo(a: String?, b: String): Boolean = a != b");
        String text = generateToText();
        assertTrue(text.contains("IXOR"));
        Method main = generateFunction();
        assertEquals(true, main.invoke(null, null, "lala"));
        assertEquals(false, main.invoke(null, "papa", "papa"));
    }
}
