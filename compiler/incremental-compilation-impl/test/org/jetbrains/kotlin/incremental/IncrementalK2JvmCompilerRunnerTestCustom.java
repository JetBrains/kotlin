/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.TargetBackend;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.runner.RunWith;

@SuppressWarnings("all")
@RunWith(JUnit3RunnerWithInners.class)
public class IncrementalK2JvmCompilerRunnerTestCustom extends AbstractIncrementalK2JvmCompilerRunnerTest {
    @TestMetadata("jps/jps-plugin/testData/incremental/custom")
    @TestDataPath("$PROJECT_ROOT")
    @RunWith(JUnit3RunnerWithInners.class)
    public static class Custom extends AbstractIncrementalK2JvmCompilerRunnerTest {
        private void runTest(String testDataFilePath) throws Exception {
            KotlinTestUtils.runTest(this::doTest, TargetBackend.JVM_IR, testDataFilePath);
        }

        @TestMetadata("companionWithSyntaxError")
        public void testCompanionWithSyntaxError() throws Exception {
            runTest("jps/jps-plugin/testData/incremental/custom/companionWithSyntaxError/");
        }
    }
}
