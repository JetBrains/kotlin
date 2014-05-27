/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

public class ArrayGenTest extends CodegenTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
    }

    public void testCreateMultiInt() throws Exception {
        loadText("fun foo() = Array<Array<Int>> (5, { Array<Int>(it, {239}) })");
        Method foo = generateFunction();
        Integer[][] invoke = (Integer[][]) foo.invoke(null);
        assertEquals(invoke[2].length, 2);
        assertEquals(invoke[4].length, 4);
        assertEquals(invoke[4][2].intValue(), 239);
    }

    public void testCreateMultiIntNullable() throws Exception {
        loadText("fun foo() = Array<Array<Int?>> (5, { arrayOfNulls<Int>(it) })");
        Method foo = generateFunction();
        Integer[][] invoke = (Integer[][]) foo.invoke(null);
        assertEquals(invoke[2].length, 2);
        assertEquals(invoke[4].length, 4);
    }

    public void testCreateMultiString() throws Exception {
        loadText("fun foo() = Array<Array<String>> (5, { Array<String>(0,{\"\"}) })");
        Method foo = generateFunction();
        Object invoke = foo.invoke(null);
        assertTrue(invoke instanceof Object[]);
    }

    public void testIntGenerics() throws Exception {
        loadText("class L<T>(var a : T) {} fun foo() = L<Int>(5).a");
        Method foo = generateFunction();
        Object invoke = foo.invoke(null);
        assertTrue(invoke instanceof Integer);
    }
}
