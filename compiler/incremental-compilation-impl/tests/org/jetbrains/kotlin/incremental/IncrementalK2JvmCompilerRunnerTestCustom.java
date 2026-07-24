/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@SuppressWarnings("all")
public class IncrementalK2JvmCompilerRunnerTestCustom extends AbstractIncrementalK2JvmCompilerRunnerTest {
    @Nested
    @TestMetadata("jps/jps-plugin/testData/incremental/custom")
    @TestDataPath("$PROJECT_ROOT")
    public class Custom extends AbstractIncrementalK2JvmCompilerRunnerTest {
        @Test
        @TestMetadata("companionWithSyntaxError")
        public void testCompanionWithSyntaxError() throws Exception {
            runTest("jps/jps-plugin/testData/incremental/custom/companionWithSyntaxError/");
        }
    }
}
