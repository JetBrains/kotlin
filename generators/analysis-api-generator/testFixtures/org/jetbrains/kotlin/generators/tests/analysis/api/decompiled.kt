/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests.analysis.api

import org.jetbrains.kotlin.analysis.decompiler.konan.AbstractAdditionalStubInfoKnmTest
import org.jetbrains.kotlin.analysis.decompiler.stub.files.AbstractAdditionalStubInfoK2CompilerTest
import org.jetbrains.kotlin.analysis.decompiler.stub.files.AbstractAdditionalStubInfoTest
import org.jetbrains.kotlin.generators.dsl.TestGroupSuite

internal fun TestGroupSuite.generateDecompiledTests() {
    testGroup(
        "analysis/decompiled/decompiler-to-file-stubs/tests-gen",
        "analysis/decompiled/decompiler-to-file-stubs/testData",
    ) {
        testClass<AbstractAdditionalStubInfoTest> {
            model("additionalClsStubInfo", extension = null, recursive = false)
        }

        testClass<AbstractAdditionalStubInfoK2CompilerTest> {
            model("additionalClsStubInfo", extension = null, recursive = false)
        }
    }

    testGroup(
        "analysis/decompiled/decompiler-native/tests-gen",
        "analysis/decompiled/decompiler-to-file-stubs/testData",
    ) {
        testClass<AbstractAdditionalStubInfoKnmTest> {
            model("additionalClsStubInfo", extension = null, recursive = false)
        }
    }
}
