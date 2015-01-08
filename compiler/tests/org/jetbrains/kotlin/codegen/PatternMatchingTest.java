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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.ConfigurationKind;

import java.lang.reflect.Method;

import static org.jetbrains.kotlin.codegen.CodegenTestUtil.assertThrows;

public class PatternMatchingTest extends CodegenTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
    }

    @NotNull
    @Override
    protected String getPrefix() {
        return "patternMatching";
    }

    public void testConstant() throws Exception {
        loadFile();
        Method foo = generateFunction();
        assertTrue((Boolean) foo.invoke(null, 0));
        assertFalse((Boolean) foo.invoke(null, 1));
    }

    public void testExceptionOnNoMatch() throws Exception {
        loadFile();
        Method foo = generateFunction();
        assertTrue((Boolean) foo.invoke(null, 0));
        assertThrows(foo, Exception.class, null, 1);
    }

    public void testPattern() throws Exception {
        loadFile();
        Method foo = generateFunction();
        assertEquals("string", foo.invoke(null, ""));
        assertEquals("something", foo.invoke(null, new Object()));
    }

    public void testInrange() throws Exception {
        loadFile();
        Method foo = generateFunction();
        assertEquals("array list", foo.invoke(null, 239));
        assertEquals("digit", foo.invoke(null, 0));
        assertEquals("digit", foo.invoke(null, 9));
        assertEquals("digit", foo.invoke(null, 5));
        assertEquals("not small", foo.invoke(null, 190));
        assertEquals("something", foo.invoke(null, 19));
    }

    public void testRangeChar() throws Exception {
        loadFile();
        Method foo = generateFunction();
        assertEquals("digit", foo.invoke(null, '0'));
        assertEquals("something", foo.invoke(null, 'A'));
    }

    public void testWildcardPattern() throws Exception {
        loadText("fun foo(x: String) = when(x) { else -> \"something\" }");
        Method foo = generateFunction();
        assertEquals("something", foo.invoke(null, ""));
    }

    public void testNoReturnType() throws Exception {
        loadText("fun foo(x: String) = when(x) { else -> \"x\" }");
        Method foo = generateFunction();
        assertEquals("x", foo.invoke(null, ""));
    }

    public void testCall() throws Exception {
        loadText("fun foo(s: String) = when { s[0] == 'J' -> \"JetBrains\"; else -> \"something\" }");
        Method foo = generateFunction();
        assertEquals("JetBrains", foo.invoke(null, "Java"));
        assertEquals("something", foo.invoke(null, "C#"));
    }

    public void testMultipleConditions() throws Exception {
        loadText("fun foo(x: Any) = when(x) { 0, 1 -> \"bit\"; else -> \"something\" }");
        Method foo = generateFunction();
        assertEquals("bit", foo.invoke(null, 0));
        assertEquals("bit", foo.invoke(null, 1));
        assertEquals("something", foo.invoke(null, 2));
    }
}
