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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author yole
 * @author alex.tkachman
 */
public class StringsTest extends CodegenTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createEnvironmentWithMockJdk(CompilerSpecialMode.JDK_HEADERS);
    }

    public void testAnyToString () throws InvocationTargetException, IllegalAccessException {
        loadText("fun foo(x: Any) = x.toString()");
//        System.out.println(generateToText());
        Method foo = generateFunction();
        assertEquals("something", foo.invoke(null, "something"));
        assertEquals("null", foo.invoke(null, new Object[]{null}));

    }

    public void testNullableAnyToString () throws InvocationTargetException, IllegalAccessException {
        loadText("fun foo(x: Any?) = x.toString()");
//        System.out.println(generateToText());
        Method foo = generateFunction();
        assertEquals("something", foo.invoke(null, "something"));
        assertEquals("null", foo.invoke(null, new Object[]{null}));

    }

    public void testNullableStringPlus () throws InvocationTargetException, IllegalAccessException {
        loadText("fun foo(x: String?, y: Any?) = x + y");
        String text = generateToText();
        assertTrue(text.contains(".stringPlus"));
//        System.out.println(text);
        Method foo = generateFunction();
        assertEquals("something239", foo.invoke(null, "something", 239));
        assertEquals("null239", foo.invoke(null, null, 239));
        assertEquals("239null", foo.invoke(null, "239", null));
        assertEquals("nullnull", foo.invoke(null, null, null));

    }

    public void testNonNullableStringPlus () throws InvocationTargetException, IllegalAccessException {
        loadText("fun foo(x: String, y: Any?) = x + y + 120");
        String text = generateToText();
        assertFalse(text.contains(".stringPlus"));
//        System.out.println(text);
        Method foo = generateFunction();
        assertEquals("something239120", foo.invoke(null, "something", 239));
        assertEquals("null239120", foo.invoke(null, null, 239));
        assertEquals("239null120", foo.invoke(null, "239", null));
        assertEquals("nullnull120", foo.invoke(null, null, null));

    }

    public void testRawStrings() throws Exception {
        blackBoxFile("rawStrings.jet");
    }

    public void testMultilineStringsWithTemplates() throws Exception {
        blackBoxFile("multilineStringsWithTemplates.jet");
    }

    public void testKt881() throws Exception {
        blackBoxFile("regressions/kt881.jet");
    }

    public void testKt894() throws Exception {
        blackBoxFile("regressions/kt894.jet");
    }

    public void testKt889() throws Exception {
        blackBoxFile("regressions/kt889.jet");
    }
}
