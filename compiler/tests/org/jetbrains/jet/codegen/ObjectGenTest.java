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
        createEnvironmentWithMockJdkAndIdeaAnnotations(CompilerSpecialMode.JDK_HEADERS);
    }

    public void testSimpleObject() {
        blackBoxFile("objects/simpleObject.jet");
//        System.out.println(generateToText());
    }

    public void testObjectLiteral() {
        blackBoxFile("objects/objectLiteral.jet");
//        System.out.println(generateToText());
    }

    public void testObjectLiteralInClosure() {
        blackBoxFile("objects/objectLiteralInClosure.jet");
//        System.out.println(generateToText());
    }

    public void testMethodOnObject() {
        blackBoxFile("objects/methodOnObject.jet");
    }

    public void testKt535() {
        blackBoxFile("regressions/kt535.jet");
    }

    public void testKt560() {
        blackBoxFile("regressions/kt560.jet");
    }

    public void testKt640() {
        blackBoxFile("regressions/kt640.jet");
    }

    public void testKt1136() {
        blackBoxFile("regressions/kt1136.kt");
    }

    public void testKt1047() {
        blackBoxFile("regressions/kt1047.kt");
    }

    public void testKt694() {
        blackBoxFile("regressions/kt694.kt");
    }

    public void testKt1186() {
        blackBoxFile("regressions/kt1186.kt");
    }

    public void testKt1600() {
        blackBoxFile("regressions/kt1600.kt");
    }

    public void testKt1737() {
        blackBoxFile("regressions/kt1737.kt");
    }
}
