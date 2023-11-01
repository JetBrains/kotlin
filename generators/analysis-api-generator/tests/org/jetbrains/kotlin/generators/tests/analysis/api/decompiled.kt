/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests.analysis.api

import org.jetbrains.kotlin.analysis.decompiler.konan.AbstractDecompiledKnmStubConsistencyFe10Test
import org.jetbrains.kotlin.analysis.decompiler.konan.AbstractDecompiledKnmStubConsistencyK2Test
import org.jetbrains.kotlin.analysis.decompiler.psi.AbstractByDecompiledPsiStubBuilderK2CompilerTest
import org.jetbrains.kotlin.analysis.decompiler.psi.AbstractByDecompiledPsiStubBuilderTest
import org.jetbrains.kotlin.analysis.decompiler.stub.files.AbstractAdditionalStubInfoK2CompilerTest
import org.jetbrains.kotlin.analysis.decompiler.stub.files.AbstractAdditionalStubInfoTest
import org.jetbrains.kotlin.analysis.decompiler.stub.files.AbstractClsStubBuilderK2CompilerTest
import org.jetbrains.kotlin.analysis.decompiler.stub.files.AbstractClsStubBuilderTest
import org.jetbrains.kotlin.generators.TestGroupSuite

internal fun TestGroupSuite.generateDecompiledTests() {
    testGroup(
        "analysis/decompiled/decompiler-to-file-stubs/tests",
        "analysis/decompiled/decompiler-to-file-stubs/testData",
    ) {
        testClass<AbstractClsStubBuilderTest> {
            model("clsFileStubBuilder", extension = null, recursive = false)
        }

        testClass<AbstractClsStubBuilderK2CompilerTest> {
            model("clsFileStubBuilder", extension = null, recursive = false)
        }
    }

    testGroup(
        "analysis/decompiled/decompiler-to-file-stubs/tests",
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
        "analysis/decompiled/decompiler-to-psi/tests",
        "analysis/decompiled/decompiler-to-file-stubs/testData",
    ) {
        testClass<AbstractByDecompiledPsiStubBuilderTest> {
            model("clsFileStubBuilder", extension = null, recursive = false)
        }

        testClass<AbstractByDecompiledPsiStubBuilderK2CompilerTest> {
            model("clsFileStubBuilder", extension = null, recursive = false)
        }
    }

    testGroup(
        "analysis/decompiled/decompiler-native/tests",
        "analysis/decompiled/decompiler-to-file-stubs/testData",
    ) {
        testClass<AbstractDecompiledKnmStubConsistencyK2Test> {
            model("clsFileStubBuilder", extension = null, recursive = false)
        }
        testClass<AbstractDecompiledKnmStubConsistencyFe10Test> {
            model("clsFileStubBuilder", extension = null, recursive = false)
        }
    }
}