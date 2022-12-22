/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests.analysis.api

import org.jetbrains.kotlin.generators.TestGroupSuite
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlin.light.classes.symbol.decompiled.*
import org.jetbrains.kotlin.light.classes.symbol.source.*

internal fun TestGroupSuite.generateSymbolLightClassesTests() {
    testGroup(
        "analysis/symbol-light-classes/tests",
        "compiler/testData",
    ) {
        run {
            testClass<AbstractSymbolLightClassesForSourceTest> {
                model(
                    "asJava/lightClasses/lightClasses",
                    pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME
                )
            }

            testClass<AbstractSymbolLightClassesForLibraryTest> {
                model(
                    "asJava/lightClasses/lightClasses",
                    excludeDirs = listOf("compilationErrors"),
                    pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME
                )
            }

            testClass<AbstractSymbolLightClassesParentingForSourceTest> {
                model(
                    "asJava/lightClasses/lightClasses",
                    pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME
                )
            }

            testClass<AbstractSymbolLightClassesParentingForLibraryTest> {
                model(
                    "asJava/lightClasses/lightClasses",
                    excludeDirs = listOf("compilationErrors"),
                    pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME
                )
            }
        }

        run {
            testClass<AbstractSymbolLightClassesLoadingForSourceTest> {
                model("asJava/lightClasses/ultraLightClasses", pattern = TestGeneratorUtil.KT_OR_KTS)
            }

            testClass<AbstractSymbolLightClassesLoadingForLibraryTest> {
                model("asJava/lightClasses/ultraLightClasses", pattern = TestGeneratorUtil.KT_OR_KTS)
            }
        }

        run {
            testClass<AbstractSymbolLightClassesFacadeForSourceTest> {
                model("asJava/lightClasses/ultraLightFacades", pattern = TestGeneratorUtil.KT_OR_KTS)
            }

            testClass<AbstractSymbolLightClassesFacadeForLibraryTest> {
                model("asJava/lightClasses/ultraLightFacades", pattern = TestGeneratorUtil.KT_OR_KTS)
            }
        }
    }

    testGroup(
        "analysis/symbol-light-classes/tests",
        "analysis/symbol-light-classes/testData",
    ) {
        run {
            testClass<AbstractSymbolLightClassesStructureForSourceTest> {
                model("structure", pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractSymbolLightClassesStructureForLibraryTest> {
                model("structure", pattern = TestGeneratorUtil.KT)
            }
        }

        run {
            testClass<AbstractSymbolLightClassesStructureByFqNameForSourceTest> {
                model("structureByFqName", pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractSymbolLightClassesStructureByFqNameForLibraryTest> {
                model("structureByFqName", pattern = TestGeneratorUtil.KT)
            }
        }
    }
}
