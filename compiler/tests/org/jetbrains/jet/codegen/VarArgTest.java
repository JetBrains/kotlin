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

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author alex.tkachman
 */
public class VarArgTest extends CodegenTestCase {
    public void testStringArray () throws InvocationTargetException, IllegalAccessException {
        createEnvironmentWithMockJdkAndIdeaAnnotations(CompilerSpecialMode.ALT_HEADERS);
        loadText("fun test(vararg ts: String) = ts");
//        System.out.println(generateToText());
        final Method main = generateFunction();
        String[] args = {"mama", "papa"};
        assertTrue(args == main.invoke(null, new Object[]{ args } ));
    }

    public void testIntArray () throws InvocationTargetException, IllegalAccessException {
        createEnvironmentWithMockJdkAndIdeaAnnotations(CompilerSpecialMode.ALT_HEADERS);
        loadText("fun test(vararg ts: Int) = ts");
//        System.out.println(generateToText());
        final Method main = generateFunction();
        int[] args = {3, 4};
        assertTrue(args == main.invoke(null, new Object[]{ args }));
    }

    public void testIntArrayKotlinNoArgs () throws InvocationTargetException, IllegalAccessException {
        createEnvironmentWithMockJdkAndIdeaAnnotations(CompilerSpecialMode.ALT_HEADERS);
        loadText("fun test() = testf(); fun testf(vararg ts: Int) = ts");
//        System.out.println(generateToText());
        final Method main = generateFunction("test");
        Object res = main.invoke(null);
        assertTrue(((int[])res).length == 0);
    }

    public void testIntArrayKotlin () throws InvocationTargetException, IllegalAccessException {
        createEnvironmentWithMockJdkAndIdeaAnnotations(CompilerSpecialMode.ALT_HEADERS);
        loadText("fun test() = testf(239, 7); fun testf(vararg ts: Int) = ts");
//        System.out.println(generateToText());
        final Method main = generateFunction("test");
        Object res = main.invoke(null);
        assertTrue(((int[])res).length == 2);
        assertTrue(((int[])res)[0] == 239);
        assertTrue(((int[])res)[1] == 7);
    }

    public void testNullableIntArrayKotlin () throws InvocationTargetException, IllegalAccessException {
        createEnvironmentWithMockJdkAndIdeaAnnotations(CompilerSpecialMode.ALT_HEADERS);
        loadText("fun test() = testf(239.toByte(), 7.toByte()); fun testf(vararg ts: Byte?) = ts");
//        System.out.println(generateToText());
        final Method main = generateFunction("test");
        Object res = main.invoke(null);
        assertTrue(((Byte[])res).length == 2);
        assertTrue(((Byte[])res)[0] == (byte)239);
        assertTrue(((Byte[])res)[1] == 7);
    }

    public void testIntArrayKotlinObj () throws InvocationTargetException, IllegalAccessException {
        createEnvironmentWithMockJdkAndIdeaAnnotations(CompilerSpecialMode.ALT_HEADERS);
        loadText("fun test() = testf(\"239\"); fun testf(vararg ts: String) = ts");
//        System.out.println(generateToText());
        final Method main = generateFunction("test");
        Object res = main.invoke(null);
        assertTrue(((String[])res).length == 1);
        assertTrue(((String[])res)[0].equals("239"));
    }

    public void testArrayT () throws InvocationTargetException, IllegalAccessException {
        createEnvironmentWithMockJdkAndIdeaAnnotations(CompilerSpecialMode.ALT_HEADERS);
        loadText("fun test() = _array(2, 4); fun <T> _array(vararg elements : T) = elements");
//        System.out.println(generateToText());
        final Method main = generateFunction("test");
        Object res = main.invoke(null);
        assertTrue(((Integer[])res).length == 2);
        assertTrue(((Integer[])res)[0].equals(2));
        assertTrue(((Integer[])res)[1].equals(4));
    }

    public void testKt581() {
        createEnvironmentWithFullJdk();
        blackBoxFile("regressions/kt581.jet");
    }

    public void testKt797() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(CompilerSpecialMode.ALT_HEADERS);
        blackBoxFile("regressions/kt796_797.jet");
    }

    public void testArrayAsVararg () throws InvocationTargetException, IllegalAccessException {
        createEnvironmentWithMockJdkAndIdeaAnnotations(CompilerSpecialMode.ALT_HEADERS);
        loadText("private fun asList(vararg elems: String) = elems; fun test(ts: Array<String>) = asList(*ts); ");
        //System.out.println(generateToText());
        final Method main = generateFunction("test");
        String[] args = {"mama", "papa"};
        assertTrue(args == main.invoke(null, new Object[]{ args } ));
    }

    public void testArrayAsVararg2 () throws InvocationTargetException, IllegalAccessException {
        createEnvironmentWithMockJdkAndIdeaAnnotations(CompilerSpecialMode.ALT_HEADERS);
        loadText("private fun asList(vararg elems: String) = elems; fun test(ts1: Array<String>, ts2: String) = asList(*ts1, ts2); ");
        System.out.println(generateToText());
        final Method main = generateFunction("test");
        Object invoke = main.invoke(null, new Object[] {new String[] {"mama"}, "papa" });
        assertInstanceOf(invoke, String[].class);
        assertEquals(2, Array.getLength(invoke));
        assertEquals("mama", Array.get(invoke, 0));
        assertEquals("papa", Array.get(invoke, 1));
    }
}
