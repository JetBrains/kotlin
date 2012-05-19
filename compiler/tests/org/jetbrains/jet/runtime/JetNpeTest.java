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

package org.jetbrains.jet.runtime;

import jet.runtime.Intrinsics;
import org.jetbrains.jet.codegen.CodegenTestCase;

import java.lang.reflect.Method;

public class JetNpeTest extends CodegenTestCase {
    public void testStackTrace () {
        try {
            Intrinsics.throwNpe();
            fail("No Sure thrown");
        }
        catch (NullPointerException e) {
            StackTraceElement stackTraceElement = e.getStackTrace()[0];
            assertEquals(stackTraceElement.getMethodName(), "testStackTrace");
            assertEquals(stackTraceElement.getClassName(), "org.jetbrains.jet.runtime.JetNpeTest");
        }
    }
    
    public void testNotNull () throws Exception {
        loadText("fun box() = if(10.sure() == 10) \"OK\" else \"fail\"");
        blackBox();
    }

    public void testNull () throws Exception {
        loadText("fun box() = if((null : Int?).sure() == 10) \"OK\" else \"fail\"");
//        System.out.println(generateToText());
        Method box = generateFunction("box");
        assertThrows(box, NullPointerException.class, null);
    }
}
