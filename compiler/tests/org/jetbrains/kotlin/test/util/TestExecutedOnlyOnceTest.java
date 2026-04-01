/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
