/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.runners;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.konan.test.blackbox.support.group.UseDummyTestCaseGroupProvider;
import org.jetbrains.kotlin.test.AbstractMyNativeTwoPhaseTest;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NewClassNamingConvention")
@TestMetadata("compiler/testData/codegen/box")
@TestDataPath("$PROJECT_ROOT")
@UseDummyTestCaseGroupProvider()
class MyNativeCodegenBoxTestGenerated extends AbstractMyNativeTwoPhaseTest {
    @Nested
    @TestMetadata("compiler/testData/codegen/box/arrays")
    @TestDataPath("$PROJECT_ROOT")
    class Arrays {
        @Nested
        @TestMetadata("compiler/testData/codegen/box/arrays/multiDecl")
        @TestDataPath("$PROJECT_ROOT")
        class MultiDecl {
            void run(String fileName) {
                initTestRunnerAndCreateModuleStructure("compiler/testData/codegen/box/arrays/multiDecl/" + fileName);
            }

            @Test
            @TestMetadata("kt15560.kt")
            void testKt15560() {
                run("kt15560.kt");
            }

            @Test
            @TestMetadata("kt15568.kt")
            void testKt15568() {
                run("kt15568.kt");
            }

            @Test
            @TestMetadata("kt15575.kt")
            void testKt15575() {
                run("kt15575.kt");
            }
        }
    }

}
