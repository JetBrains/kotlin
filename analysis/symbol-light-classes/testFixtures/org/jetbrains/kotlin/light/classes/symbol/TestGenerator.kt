/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol

import org.jetbrains.kotlin.generators.dsl.TestGroup
import org.jetbrains.kotlin.generators.dsl.TestGroupSuite
import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlin.light.classes.symbol.base.AbstractLightClassUtilTest
import org.jetbrains.kotlin.light.classes.symbol.base.AbstractSymbolLightClassesEquivalentTest
import org.jetbrains.kotlin.light.classes.symbol.decompiled.*
import org.jetbrains.kotlin.light.classes.symbol.source.*

fun main(args: Array<String>) {
    generateTestGroupSuiteWithJUnit5(args) {
        generateCompilerTestDataBasedLightClassesTests()

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
                    model("annotationsEquality", pattern = TestGeneratorUtil.KT)
                }
            }

            run {
                testClass<AbstractLightClassUtilTest> {
                    model("lightElements", pattern = TestGeneratorUtil.KT)
                }
            }

            run {
                testClass<AbstractSymbolLightClassesNestedClassesConsistencyForLibraryTest> {
                    model("libraryNestedClassesConsistency", pattern = TestGeneratorUtil.KT)
                }
            }
        }
    }
}

private fun TestGroupSuite.generateCompilerTestDataBasedLightClassesTests() {
    testGroup(
        "analysis/symbol-light-classes/tests-gen",
        "compiler/testData",
    ) {
        lightClassesByFqNameTests()
        lightClassesByPsiTests()
    }
}

private fun lightClassesTestsInit(
    path: String,
    isLibrary: Boolean,
    isScript: Boolean,
): TestGroup.TestClass.() -> Unit = {
    model(
        relativeRootPath = path,
        excludeDirs = if (isLibrary) listOf("compilationErrors") else emptyList(),
        pattern = if (isScript) TestGeneratorUtil.KTS else TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME,
    )
}

private fun lightClassesByPsiTestsInit(
    isLibrary: Boolean = false,
    isScript: Boolean = false,
): TestGroup.TestClass.() -> Unit = lightClassesTestsInit(
    path = "asJava/lightClasses/lightClassByPsi",
    isLibrary = isLibrary,
    isScript = isScript,
)

private fun TestGroup.lightClassesByPsiTests() {
    val sourceModelInit = lightClassesByPsiTestsInit()
    val libraryModelInit = lightClassesByPsiTestsInit(isLibrary = true)
    val scriptModelInit = lightClassesByPsiTestsInit(isScript = true)

    testClass<AbstractSymbolLightClassesByPsiForSourceTest>(init = sourceModelInit)
    testClass<AbstractJsSymbolLightClassesByPsiForSourceTest>(init = sourceModelInit)
    testClass<AbstractScriptSymbolLightClassesByPsiForSourceTest>(init = scriptModelInit)

    testClass<AbstractSymbolLightClassesByPsiForLibraryTest>(init = libraryModelInit)
    testClass<AbstractJsSymbolLightClassesByPsiForLibraryTest>(init = libraryModelInit)

    testClass<AbstractSymbolLightClassesMatcherByPsiForLibraryTest>(init = libraryModelInit)

    testClass<AbstractSymbolLightClassesParentingByPsiForSourceTest>(init = sourceModelInit)
    testClass<AbstractSymbolLightClassesParentingByPsiForLibraryTest>(init = libraryModelInit)
    testClass<AbstractScriptSymbolLightClassesParentingByPsiForSourceTest>(init = scriptModelInit)

    testClass<AbstractSymbolLightClassesEqualityByPsiForSourceTest>(init = sourceModelInit)
    testClass<AbstractSymbolLightClassesEqualityByPsiForLibraryTest>(init = libraryModelInit)
    testClass<AbstractScriptSymbolLightClassesEqualityByPsiForSourceTest>(init = scriptModelInit)
}

private fun lightClassesByFqNameTestsInit(
    isLibrary: Boolean = false,
    isScript: Boolean = false,
): TestGroup.TestClass.() -> Unit = lightClassesTestsInit(
    path = "asJava/lightClasses/lightClassByFqName",
    isLibrary = isLibrary,
    isScript = isScript,
)

private fun TestGroup.lightClassesByFqNameTests() {
    val sourceModelInit = lightClassesByFqNameTestsInit()
    val libraryModelInit = lightClassesByFqNameTestsInit(isLibrary = true)
    val scriptModelInit = lightClassesByFqNameTestsInit(isScript = true)

    testClass<AbstractSymbolLightClassesByFqNameForSourceTest>(init = sourceModelInit)
    testClass<AbstractJsSymbolLightClassesByFqNameForSourceTest>(init = sourceModelInit)
    testClass<AbstractScriptSymbolLightClassesByFqNameForSourceTest>(init = scriptModelInit)

    testClass<AbstractSymbolLightClassesByFqNameForLibraryTest>(init = libraryModelInit)
    testClass<AbstractJsSymbolLightClassesByFqNameForLibraryTest>(init = libraryModelInit)

    testClass<AbstractSymbolLightClassesParentingByFqNameForSourceTest>(init = sourceModelInit)
    testClass<AbstractSymbolLightClassesParentingByFqNameForLibraryTest>(init = libraryModelInit)
    testClass<AbstractScriptSymbolLightClassesParentingByFqNameForSourceTest>(init = scriptModelInit)

    testClass<AbstractSymbolLightClassesEqualityByFqNameForSourceTest>(init = sourceModelInit)
    testClass<AbstractSymbolLightClassesEqualityByFqNameForLibraryTest>(init = libraryModelInit)
    testClass<AbstractScriptSymbolLightClassesEqualityByFqNameForSourceTest>(init = scriptModelInit)
}
