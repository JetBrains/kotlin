/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests.analysis.api

import org.jetbrains.kotlin.generators.TestGroupSuite
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlin.light.classes.symbol.decompiled.AbstractSymbolLightClassesParentingForLibraryTest
import org.jetbrains.kotlin.light.classes.symbol.decompiled.AbstractSymbolLightClassesFacadeForLibraryTest
import org.jetbrains.kotlin.light.classes.symbol.decompiled.AbstractSymbolLightClassesForLibraryTest
import org.jetbrains.kotlin.light.classes.symbol.decompiled.AbstractSymbolLightClassesLoadingForLibraryTest
import org.jetbrains.kotlin.light.classes.symbol.source.AbstractSymbolLightClassesParentingForSourceTest
import org.jetbrains.kotlin.light.classes.symbol.source.AbstractSymbolLightClassesFacadeForSourceTest
import org.jetbrains.kotlin.light.classes.symbol.source.AbstractSymbolLightClassesForSourceTest
import org.jetbrains.kotlin.light.classes.symbol.source.AbstractSymbolLightClassesLoadingForSourceTest

internal fun TestGroupSuite.generateSymbolLightClassesTests() {
    testGroup(
        "analysis/symbol-light-classes/tests",
        "compiler/testData",
    ) {
        run {
            testClass<AbstractSymbolLightClassesForSourceTest> {
                model(
                    "asJava/lightClasses",
                    pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME
                )
            }

            testClass<AbstractSymbolLightClassesForLibraryTest> {
                model(
                    "asJava/lightClasses",
                    excludeDirs = listOf("compilationErrors"),
                    pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME
                )
            }

            testClass<AbstractSymbolLightClassesParentingForSourceTest> {
                model(
                    "asJava/lightClasses",
                    pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME
                )
            }

            testClass<AbstractSymbolLightClassesParentingForLibraryTest> {
                model(
                    "asJava/lightClasses",
                    excludeDirs = listOf("compilationErrors"),
                    pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME
                )
            }
        }

        run {
            testClass<AbstractSymbolLightClassesLoadingForSourceTest> {
                model("asJava/ultraLightClasses", pattern = TestGeneratorUtil.KT_OR_KTS)
            }

            testClass<AbstractSymbolLightClassesLoadingForLibraryTest> {
                model("asJava/ultraLightClasses", pattern = TestGeneratorUtil.KT_OR_KTS)
            }
        }

        run {
            testClass<AbstractSymbolLightClassesFacadeForSourceTest> {
                model("asJava/ultraLightFacades", pattern = TestGeneratorUtil.KT_OR_KTS)
            }

            testClass<AbstractSymbolLightClassesFacadeForLibraryTest> {
                model("asJava/ultraLightFacades", pattern = TestGeneratorUtil.KT_OR_KTS)
            }
        }
    }
}