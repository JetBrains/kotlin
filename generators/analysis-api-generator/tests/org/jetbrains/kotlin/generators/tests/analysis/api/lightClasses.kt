/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests.analysis.api

import org.jetbrains.kotlin.generators.TestGroupSuite
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlin.light.classes.symbol.base.AbstractSymbolLightClassesEqualityTest
import org.jetbrains.kotlin.light.classes.symbol.decompiled.*
import org.jetbrains.kotlin.light.classes.symbol.source.*

internal fun TestGroupSuite.generateSymbolLightClassesTests() {
    testGroup(
        "analysis/symbol-light-classes/tests",
        "compiler/testData",
    ) {
        run {
            testClass<AbstractSymbolLightClassesByFqNameForSourceTest> {
                model(
                    "asJava/lightClasses/lightClassByFqName",
                    pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME
                )
            }

            testClass<AbstractSymbolLightClassesByFqNameForLibraryTest> {
                model(
                    "asJava/lightClasses/lightClassByFqName",
                    excludeDirs = listOf("compilationErrors"),
                    pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME
                )
            }

            testClass<AbstractSymbolLightClassesParentingForSourceTest> {
                model(
                    "asJava/lightClasses/lightClassByFqName",
                    pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME
                )
            }

            testClass<AbstractSymbolLightClassesParentingForLibraryTest> {
                model(
                    "asJava/lightClasses/lightClassByFqName",
                    excludeDirs = listOf("compilationErrors"),
                    pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME
                )
            }
        }

        run {
            testClass<AbstractSymbolLightClassesByPsiForSourceTest> {
                model("asJava/lightClasses/lightClassByPsi", pattern = TestGeneratorUtil.KT_OR_KTS)
            }

            testClass<AbstractSymbolLightClassesByPsiForLibraryTest> {
                model("asJava/lightClasses/lightClassByPsi", pattern = TestGeneratorUtil.KT_OR_KTS)
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

        run {
            testClass<AbstractSymbolLightClassesEqualityTest> {
                model("equivalentTo", pattern = TestGeneratorUtil.KT)
            }
        }

        run {
            testClass<AbstractSymbolLightClassesAnnotationEqualityForSourceTest> {
                model("annotationsEquality", pattern = TestGeneratorUtil.KT)
            }
        }
    }
}
