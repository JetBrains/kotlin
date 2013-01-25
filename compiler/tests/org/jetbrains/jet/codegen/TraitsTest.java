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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.ConfigurationKind;

public class TraitsTest extends CodegenTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
    }

    @NotNull
    @Override
    protected String getPrefix() {
        return "traits";
    }

    public void testSimple () {
        blackBoxFile("traits/simple.kt");
    }

    public void testWithRequired () {
        blackBoxFile("traits/withRequired.kt");
    }

    public void testMultiple () {
        blackBoxFile("traits/multiple.kt");
    }

    public void testStdlib () {
        blackBoxFile("traits/stdlib.kt");
    }

    public void testInheritedFun() {
        blackBoxFile("traits/inheritedFun.kt");
    }

    public void testInheritedVar() {
        blackBoxFile("traits/inheritedVar.kt");
    }
    
    public void testKt2399() {
        blackBoxFile("regressions/kt2399.kt");
    }

    public void testTraitFuncCall() {
        blackBoxFile("traits/traitFuncCall.kt");
    }

    public void testKt2541() {
        blackBoxFile("regressions/kt2541.kt");
    }

    public void testKt2260() {
        blackBoxFile("regressions/kt2260.kt");
    }

    public void testFinalMethod() throws Exception {
        blackBoxFile("traits/finalMethod.kt");
    }

    public void testKt1936_1() throws Exception {
        blackBoxFile("regressions/kt1936_1.kt");
    }

    public void testKt1936_2() throws Exception {
        blackBoxFile("regressions/kt1936_2.kt");
    }

    public void testKt2963() {
        blackBoxFile("regressions/kt2963.kt");
    }

    public void testWithRequiredSuper() {
        blackBoxFile("traits/withRequiredSuper.kt");
    }

    public void testWithRequiredSuperViaBridge() {
        blackBoxFile("traits/withRequiredSuperViaBridge.kt");
    }
}
