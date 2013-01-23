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

import java.lang.reflect.Method;

public class TypeInfoTest extends CodegenTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
    }

    @Override
    protected String getPrefix() {
        return "typeInfo";
    }

    public void testAsSafeOperator() throws Exception {
        loadText("fun foo(x: Any) = x as? Runnable");
        Method foo = generateFunction();
        assertNull(foo.invoke(null, new Object()));
        Runnable r = newRunnable();
        assertSame(r, foo.invoke(null, r));
    }

    public void testAsOperator() throws Exception {
        loadText("fun foo(x: Any) = x as Runnable");
        Method foo = generateFunction();
        Runnable r = newRunnable();
        assertSame(r, foo.invoke(null, r));
        assertThrows(foo, ClassCastException.class, null, new Object());
    }

    public void testIsOperator() throws Exception {
        loadText("fun foo(x: Any) = x is Runnable");
        Method foo = generateFunction();
        assertFalse((Boolean) foo.invoke(null, new Object()));
        assertTrue((Boolean) foo.invoke(null, newRunnable()));
    }

    public void testNotIsOperator() throws Exception {
        loadText("fun foo(x: Any) = x !is Runnable");
        Method foo = generateFunction();
        assertTrue((Boolean) foo.invoke(null, new Object()));
        assertFalse((Boolean) foo.invoke(null, newRunnable()));
    }

    public void testAsInLoop() {
        blackBoxFile("typeInfo/asInLoop.kt");
    }

    public void testPrimitiveTypeInfo() {
        blackBoxFile("typeInfo/primitiveTypeInfo.kt");
    }

    private Runnable newRunnable() {
        return new Runnable() {
            @Override
            public void run() {
            }
        };
    }

    public void testKt511() {
        blackBoxFile("regressions/kt511.kt");
    }

    public void testKt2811() {
        blackBoxFile("typeInfo/kt2811.kt");
    }

    public void testInheritance() {
        blackBoxFile("typeInfo/inheritance.kt");
    }
}
