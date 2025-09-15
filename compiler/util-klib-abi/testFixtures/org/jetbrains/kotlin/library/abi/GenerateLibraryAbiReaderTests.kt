/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.abi

import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5

fun main(args: Array<String>) {
    val testsRoot = args[0]

    generateTestGroupSuiteWithJUnit5(args) {
        testGroup(testsRoot, "compiler/testData/klib/dump-abi") {
            testClass<AbstractFirJsLibraryAbiReaderTest> {
                model("content")
            }
            testClass<AbstractFirJsLibraryAbiReaderWithInlinedFunInKlibTest> {
                model("content")
            }
            testClass<AbstractClassicJsLibraryAbiReaderTest> {
                model("content")
            }
        }
    }
}
