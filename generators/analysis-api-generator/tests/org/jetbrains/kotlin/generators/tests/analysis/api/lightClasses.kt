/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests.analysis.api

import org.jetbrains.kotlin.generators.TestGroup
import org.jetbrains.kotlin.generators.TestGroupSuite
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlin.light.classes.symbol.base.AbstractLightClassUtilTest
import org.jetbrains.kotlin.light.classes.symbol.base.AbstractSymbolLightClassesEquivalentTest
import org.jetbrains.kotlin.light.classes.symbol.decompiled.*
import org.jetbrains.kotlin.light.classes.symbol.source.*

internal fun TestGroupSuite.generateSymbolLightClassesTests() {
    generateCompilerTestDataBasedLightClassesTests()

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
            testClass<AbstractSymbolLightClassesEquivalentTest> {
                model("equivalentTo", pattern = TestGeneratorUtil.KT)
            }
        }

        run {
            testClass<AbstractSymbolLightClassesAnnotationEqualityForSourceTest> {
                model("annotationsEquality", pattern = TestGeneratorUtil.KT)
            }
        }

        run {
            testClass<AbstractLightClassUtilTest> {
                model("lightElements", pattern = TestGeneratorUtil.KT)
            }
        }
    }
}

private fun TestGroupSuite.generateCompilerTestDataBasedLightClassesTests() {
    testGroup(
        "analysis/symbol-light-classes/tests",
        "compiler/testData",
    ) {
        lightClassesByFqNameTests()
        lightClassesByPsiTests()
    }
}

private fun TestGroup.lightClassesByPsiTests() {
    val modelInit: TestGroup.TestClass.() -> Unit = {
        model("asJava/lightClasses/lightClassByPsi", pattern = TestGeneratorUtil.KT_OR_KTS)
    }

    testClass<AbstractSymbolLightClassesByPsiForSourceTest>(init = modelInit)
    testClass<AbstractSymbolLightClassesByPsiForLibraryTest>(init = modelInit)

    testClass<AbstractSymbolLightClassesParentingByPsiForSourceTest>(init = modelInit)
    testClass<AbstractSymbolLightClassesParentingByPsiForLibraryTest>(init = modelInit)

    testClass<AbstractSymbolLightClassesEqualityByPsiForSourceTest>(init = modelInit)
    testClass<AbstractSymbolLightClassesEqualityByPsiForLibraryTest>(init = modelInit)
}

private fun TestGroup.lightClassesByFqNameTests() {
    val sourceModelInit: TestGroup.TestClass.() -> Unit = {
        model(
            "asJava/lightClasses/lightClassByFqName",
            pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME
        )
    }

    val libraryModelInit: TestGroup.TestClass.() -> Unit = {
        model(
            "asJava/lightClasses/lightClassByFqName",
            excludeDirs = listOf("compilationErrors"),
            pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME
        )
    }

    testClass<AbstractSymbolLightClassesByFqNameForSourceTest>(init = sourceModelInit)
    testClass<AbstractSymbolLightClassesByFqNameForLibraryTest>(init = libraryModelInit)

    testClass<AbstractSymbolLightClassesParentingByFqNameForSourceTest>(init = sourceModelInit)
    testClass<AbstractSymbolLightClassesParentingByFqNameForLibraryTest>(init = libraryModelInit)

    testClass<AbstractSymbolLightClassesEqualityByFqNameForSourceTest>(init = sourceModelInit)
    testClass<AbstractSymbolLightClassesEqualityByFqNameForLibraryTest>(init = libraryModelInit)
}
