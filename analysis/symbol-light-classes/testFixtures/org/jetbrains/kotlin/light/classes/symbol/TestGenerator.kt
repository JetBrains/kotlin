/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol

import org.jetbrains.kotlin.generators.dsl.TestGroup
import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlin.light.classes.symbol.base.AbstractLightClassUtilTest
import org.jetbrains.kotlin.light.classes.symbol.base.AbstractSymbolLightClassesEquivalentTest
import org.jetbrains.kotlin.light.classes.symbol.decompiled.*
import org.jetbrains.kotlin.light.classes.symbol.source.*

fun main(args: Array<String>) {
    generateTestGroupSuiteWithJUnit5(args) {
        testGroup(
            "analysis/symbol-light-classes/tests-gen",
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
                    model("annotationsEquality", pattern = TestGeneratorUtil.KT_OR_KTS)
                }
            }

            run {
                testClass<AbstractLightClassUtilTest> {
                    model("lightElements", pattern = TestGeneratorUtil.KT_OR_KTS)
                }
            }

            run {
                testClass<AbstractSymbolLightClassesNestedClassesConsistencyForLibraryTest> {
                    model("libraryNestedClassesConsistency", pattern = TestGeneratorUtil.KT)
                }
            }

            lightClassesByFqNameTests()
            lightClassesByPsiTests()
        }
    }
}

private fun lightClassesTestsInit(
    path: String,
    isLibrary: Boolean,
): TestGroup.TestClass.() -> Unit = {
    model(
        relativeRootPath = path,
        excludeDirs = if (isLibrary) listOf("compilationErrors") else emptyList(),
        pattern = if (isLibrary) TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME else TestGeneratorUtil.KT_OR_KTS_WITHOUT_DOTS_IN_NAME,
    )
}

private fun lightClassesByPsiTestsInit(
    isLibrary: Boolean,
): TestGroup.TestClass.() -> Unit = lightClassesTestsInit(
    path = "lightClassByPsi",
    isLibrary = isLibrary,
)

private fun TestGroup.lightClassesByPsiTests() {
    val sourceModelInit = lightClassesByPsiTestsInit(isLibrary = false)
    val libraryModelInit = lightClassesByPsiTestsInit(isLibrary = true)

    testClass<AbstractSymbolLightClassesByPsiForSourceTest>(init = sourceModelInit)
    testClass<AbstractJsSymbolLightClassesByPsiForSourceTest>(init = sourceModelInit)

    testClass<AbstractSymbolLightClassesByPsiForLibraryTest>(init = libraryModelInit)
    testClass<AbstractJsSymbolLightClassesByPsiForLibraryTest>(init = libraryModelInit)

    testClass<AbstractSymbolLightClassesMatcherByPsiForLibraryTest>(init = libraryModelInit)

    testClass<AbstractSymbolLightClassesParentingByPsiForSourceTest>(init = sourceModelInit)
    testClass<AbstractSymbolLightClassesParentingByPsiForLibraryTest>(init = libraryModelInit)

    testClass<AbstractSymbolLightClassesEqualityByPsiForSourceTest>(init = sourceModelInit)
    testClass<AbstractSymbolLightClassesEqualityByPsiForLibraryTest>(init = libraryModelInit)
}

private fun lightClassesByFqNameTestsInit(
    isLibrary: Boolean,
): TestGroup.TestClass.() -> Unit = lightClassesTestsInit(
    path = "lightClassByFqName",
    isLibrary = isLibrary,
)

private fun TestGroup.lightClassesByFqNameTests() {
    val sourceModelInit = lightClassesByFqNameTestsInit(isLibrary = false)
    val libraryModelInit = lightClassesByFqNameTestsInit(isLibrary = true)

    testClass<AbstractSymbolLightClassesByFqNameForSourceTest>(init = sourceModelInit)
    testClass<AbstractJsSymbolLightClassesByFqNameForSourceTest>(init = sourceModelInit)

    testClass<AbstractSymbolLightClassesByFqNameForLibraryTest>(init = libraryModelInit)
    testClass<AbstractJsSymbolLightClassesByFqNameForLibraryTest>(init = libraryModelInit)

    testClass<AbstractSymbolLightClassesParentingByFqNameForSourceTest>(init = sourceModelInit)
    testClass<AbstractSymbolLightClassesParentingByFqNameForLibraryTest>(init = libraryModelInit)

    testClass<AbstractSymbolLightClassesEqualityByFqNameForSourceTest>(init = sourceModelInit)
    testClass<AbstractSymbolLightClassesEqualityByFqNameForLibraryTest>(init = libraryModelInit)
}
