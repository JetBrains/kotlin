/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.eval4j.test;

import junit.framework.TestSuite;
import org.jetbrains.eval4j.jdi.test.JdiTestKt;

public class Eval4jTest extends TestSuite {

    @SuppressWarnings({"UnnecessaryFullyQualifiedName", "StaticMethodReferencedViaSubclass"})
    public static TestSuite suite() {
        TestSuite eval4jSuite = new TestSuite("Eval4j Tests");
        eval4jSuite.addTest(JdiTestKt.suite());
        eval4jSuite.addTest(MainKt.suite());
        return eval4jSuite;
    }
}
