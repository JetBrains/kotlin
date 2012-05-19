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

import java.lang.reflect.Method;

/**
 * @author yole
 * @author alex.tkachman
 */
public class ExtensionFunctionsTest extends CodegenTestCase {
    @Override
    protected String getPrefix() {
        return "extensionFunctions";
    }

    public void testSimple() throws Exception {
        createEnvironmentWithMockJdk();
        loadFile();
        final Method foo = generateFunction("foo");
        final Character c = (Character) foo.invoke(null);
        assertEquals('f', c.charValue());
    }

    public void testWhenFail() throws Exception {
        createEnvironmentWithMockJdk();
        loadFile();
//        System.out.println(generateToText());
        Method foo = generateFunction("foo");
        assertThrows(foo, Exception.class, null, new StringBuilder());
    }

    public void testVirtual() throws Exception {
        createEnvironmentWithMockJdk();
        blackBoxFile("extensionFunctions/virtual.jet");
    }

    public void testShared() throws Exception {
        createEnvironmentWithMockJdk();
        blackBoxFile("extensionFunctions/shared.kt");
//        System.out.println(generateToText());
    }

    public void testKt475() throws Exception {
        createEnvironmentWithMockJdk();
        blackBoxFile("regressions/kt475.jet");
    }

    public void testKtNested() throws Exception {
        createEnvironmentWithFullJdk();
        blackBoxFile("extensionFunctions/nested.kt");
    }

    public void testKt865() throws Exception {
        createEnvironmentWithFullJdk();
        blackBoxFile("regressions/kt865.jet");
    }
}
