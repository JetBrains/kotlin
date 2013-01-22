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

public class ObjectGenTest extends CodegenTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
    }

    public void testSimpleObject() {
        blackBoxFile("objects/simpleObject.kt");
//        System.out.println(generateToText());
    }

    public void testObjectLiteral() {
        blackBoxFile("objects/objectLiteral.kt");
//        System.out.println(generateToText());
    }

    public void testObjectLiteralInClosure() {
        blackBoxFile("objects/objectLiteralInClosure.kt");
//        System.out.println(generateToText());
    }

    public void testMethodOnObject() {
        blackBoxFile("objects/methodOnObject.kt");
    }

    public void testKt535() {
        blackBoxFile("regressions/kt535.kt");
    }

    public void testKt560() {
        blackBoxFile("regressions/kt560.kt");
    }

    public void testKt640() {
        blackBoxFile("regressions/kt640.kt");
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

    public void testK2719() {
        blackBoxFile("regressions/kt2719.kt");
    }

    public void testReceiverInConstructor() {
        blackBoxFile("objects/receiverInConstructor.kt");
    }

    public void testThisInConstructor() {
        blackBoxFile("objects/thisInConstructor.kt");
    }

    public void testFlist() {
        blackBoxFile("objects/flist.kt");
    }

    public void testKt2398() {
        blackBoxFile("regressions/kt2398.kt", "OKKO");
    }

    public void testKt2675() {
        blackBoxFile("regressions/kt2675.kt");
    }

    public void testKt2663() {
        blackBoxFile("regressions/kt2663.kt");
    }

    public void testKt2663_2() {
        blackBoxFile("regressions/kt2663_2.kt");
    }

    public void testKt2822() {
        blackBoxFile("regressions/kt2822.kt");
    }

    public void testKt3238() {
        blackBoxFile("regressions/kt3238.kt");
    }
}
