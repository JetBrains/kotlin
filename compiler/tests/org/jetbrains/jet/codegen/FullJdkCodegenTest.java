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

public class FullJdkCodegenTest extends CodegenTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createEnvironmentWithFullJdk();
    }

    public void testKt434() {
        blackBoxFile("regressions/kt434.kt");
    }

    public void testSynchronized() {
        blackBoxFile("controlStructures/sync.kt");
    }

    public void testIfInWhile() {
        blackBoxFile("controlStructures/ifInWhile.kt");
    }

    public void testKt2423() {
        blackBoxFile("regressions/kt2423.kt");
    }

    public void testKt2509() {
        blackBoxFile("regressions/kt2509.kt");
    }

    public void testIntCountDownLatchExtension() {
        blackBoxFile("extensionFunctions/intCountDownLatchExtension.kt");
    }
}
