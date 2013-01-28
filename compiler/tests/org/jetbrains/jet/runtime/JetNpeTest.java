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

package org.jetbrains.jet.runtime;

import com.intellij.testFramework.UsefulTestCase;
import jet.runtime.Intrinsics;

public class JetNpeTest extends UsefulTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

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
}
