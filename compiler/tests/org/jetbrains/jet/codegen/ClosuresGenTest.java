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

public class ClosuresGenTest extends CodegenTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
    }

    public void testSimplestClosure() throws Exception {
        blackBoxFile("classes/simplestClosure.kt");
//        System.out.println(generateToText());
    }

    public void testSimplestClosureAndBoxing() throws Exception {
        blackBoxFile("classes/simplestClosureAndBoxing.kt");
    }

    public void testClosureWithParameter() throws Exception {
        blackBoxFile("classes/closureWithParameter.kt");
    }

    public void testClosureWithParameterAndBoxing() throws Exception {
        blackBoxFile("classes/closureWithParameterAndBoxing.kt");
    }

    public void testExtensionClosure() throws Exception {
        blackBoxFile("classes/extensionClosure.kt");
    }

    public void testEnclosingLocalVariable() throws Exception {
        blackBoxFile("classes/enclosingLocalVariable.kt");
//        System.out.println(generateToText());
    }

    public void testDoubleEnclosedLocalVariable() throws Exception {
        blackBoxFile("classes/doubleEnclosedLocalVariable.kt");
    }

    public void testEnclosingThis() throws Exception {
        blackBoxFile("classes/enclosingThis.kt");
    }

    public void testKt2151() {
        blackBoxFile("regressions/kt2151.kt");
    }

    public void testRecursiveClosure() {
        blackBoxFile("classes/recursiveClosure.kt");
    }
}
