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

import org.jetbrains.kotlin.test.ConfigurationKind;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class FunctionGenTest extends CodegenTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
    }

    public void testAnyEqualsNullable() throws InvocationTargetException, IllegalAccessException {
        loadText("fun foo(x: Any?) = x?.equals(\"lala\")");
        Method foo = generateFunction();
        assertTrue((Boolean) foo.invoke(null, "lala"));
        assertFalse((Boolean) foo.invoke(null, "mama"));
    }

    public void testNoRefToOuter() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        loadText("class A() { fun f() : ()->String { val s = \"OK\"; return { -> s } } }");
        Class<?> foo = generateClass("A");
        Object obj = foo.newInstance();
        Method f = foo.getMethod("f");
        Object closure = f.invoke(obj);
        Class<?> aClass = closure.getClass();
        Field[] fields = aClass.getDeclaredFields();
        assertEquals(1, fields.length);
        assertEquals("$s", fields[0].getName());
    }

    public void testAnyEquals() throws InvocationTargetException, IllegalAccessException {
        loadText("fun foo(x: Any) = x.equals(\"lala\")");
        Method foo = generateFunction();
        assertTrue((Boolean) foo.invoke(null, "lala"));
        assertFalse((Boolean) foo.invoke(null, "mama"));
    }
}
