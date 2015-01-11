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

package org.jetbrains.kotlin.test.util;

import junit.framework.TestCase;
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners;
import org.junit.runner.RunWith;

@SuppressWarnings("ALL")
@RunWith(JUnit3RunnerWithInners.class)
public class TestExecutedOnlyOnceTest extends TestCase {
    public static boolean testA = false;

    public void testA() throws Exception {
        assertFalse(testA);
        testA = true;
    }

    @RunWith(JUnit3RunnerWithInners.class)
    public static class InnerTest extends TestCase {
        private static boolean testB;
        private static boolean testC;

        public void testB() throws Exception {
            assertFalse(testB);
            testB = true;
        }

        public void testC() throws Exception {
            assertFalse(testC);
            testC = true;
        }

        @RunWith(JUnit3RunnerWithInners.class)
        public static class InnerInnerTest extends TestCase {
            @RunWith(JUnit3RunnerWithInners.class)
            public static class InnerInnerInnerTest extends TestCase {
                private static boolean testD;
                private static boolean testE;

                public void testD() throws Exception {
                    assertFalse(testD);
                    testD = true;
                }

                public void testE() throws Exception {
                    assertFalse(testE);
                    testE = true;
                }
            }
        }
    }
}
