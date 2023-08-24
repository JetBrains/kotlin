/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.abi

import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5

fun main() {
    System.setProperty("java.awt.headless", "true")

    generateTestGroupSuiteWithJUnit5 {
        testGroup("compiler/util-klib-abi/tests-gen", "compiler/testData/klib/dump-abi") {
            testClass<AbstractFirJsLibraryAbiReaderTest> {
                model("content")
            }
            testClass<AbstractClassicJsLibraryAbiReaderTest> {
                model("content")
            }
        }
    }
}
