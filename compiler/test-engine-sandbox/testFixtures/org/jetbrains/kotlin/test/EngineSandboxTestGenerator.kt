/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.model.annotation
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlin.konan.test.blackbox.support.group.UseDummyTestCaseGroupProvider

fun main(args: Array<String>) {
    val mainClassName = TestGeneratorUtil.getMainClassName()
    generateTestGroupSuiteWithJUnit5(args, mainClassName) {
        testGroup(args[0], "compiler/testData") {
            testClass<AbstractMyNativeTwoPhaseTest>(annotations = listOf(annotation(UseDummyTestCaseGroupProvider::class.java))) {
                model("codegen/box")
                model("codegen/boxInline")
            }
        }
    }
}
