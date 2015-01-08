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

import org.jetbrains.jet.ConfigurationKind;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class StringsTest extends CodegenTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
    }

    public void testAnyToString () throws InvocationTargetException, IllegalAccessException {
        loadText("fun foo(x: Any) = x.toString()");
        Method foo = generateFunction();
        assertEquals("something", foo.invoke(null, "something"));
    }

    public void testNullableAnyToString () throws InvocationTargetException, IllegalAccessException {
        loadText("fun foo(x: Any?) = x.toString()");
        Method foo = generateFunction();
        assertEquals("something", foo.invoke(null, "something"));
        assertEquals("null", foo.invoke(null, new Object[]{null}));

    }

    public void testNullableStringPlus () throws InvocationTargetException, IllegalAccessException {
        loadText("fun foo(x: String?, y: Any?) = x + y");
        String text = generateToText();
        assertTrue(text.contains(".stringPlus"));
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
        Method foo = generateFunction();
        assertEquals("something239120", foo.invoke(null, "something", 239));
        assertEquals("239null120", foo.invoke(null, "239", null));

    }
}
