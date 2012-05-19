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

/**
 * @author yole
 * @author alex.tkachman
 */
public class ObjectGenTest extends CodegenTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createEnvironmentWithMockJdk(CompilerSpecialMode.JDK_HEADERS);
    }

    public void testSimpleObject() throws Exception {
        blackBoxFile("objects/simpleObject.jet");
//        System.out.println(generateToText());
    }

    public void testObjectLiteral() throws Exception {
        blackBoxFile("objects/objectLiteral.jet");
//        System.out.println(generateToText());
    }

    public void testObjectLiteralInClosure() throws Exception {
        blackBoxFile("objects/objectLiteralInClosure.jet");
//        System.out.println(generateToText());
    }

    public void testMethodOnObject() throws Exception {
        blackBoxFile("objects/methodOnObject.jet");
    }

    public void testKt535() throws Exception {
        blackBoxFile("regressions/kt535.jet");
    }

    public void testKt560() throws Exception {
        blackBoxFile("regressions/kt560.jet");
    }

    public void testKt640() throws Exception {
        blackBoxFile("regressions/kt640.jet");
    }

    public void testKt1136() throws Exception {
        blackBoxFile("regressions/kt1136.kt");
    }

    public void testKt1047() throws Exception {
        blackBoxFile("regressions/kt1047.kt");
    }

    public void testKt694() throws Exception {
        blackBoxFile("regressions/kt694.kt");
    }

    public void testKt1186() throws Exception {
        blackBoxFile("regressions/kt1186.kt");
    }
}
