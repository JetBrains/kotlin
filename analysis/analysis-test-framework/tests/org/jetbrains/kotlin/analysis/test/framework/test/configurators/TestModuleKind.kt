/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.test.configurators

import org.jetbrains.kotlin.analysis.test.framework.AnalysisApiTestDirectives
import org.jetbrains.kotlin.test.directives.model.singleOrZeroValue
import org.jetbrains.kotlin.test.model.TestModule

/**
 * In an Analysis API test configuration, the [TestModuleKind] determines the kind of the default
 * [KaModule][org.jetbrains.kotlin.analysis.api.projectStructure.KaModule]s used in the test. This essentially defines the context in which a
 * test file is analyzed.
 *
 * The test module kind can also be overridden for a specific test module in multi-module tests using the
 * [MODULE_KIND][org.jetbrains.kotlin.analysis.test.framework.AnalysisApiTestDirectives.MODULE_KIND] directive. This allows e.g. source
 * module tests to refer to binary library dependencies.
 */
enum class TestModuleKind(val suffix: String) {
    /**
     * @see org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtSourceTestModuleFactory
     */
    Source("Source"),

    /**
     * @see org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtLibraryBinaryTestModuleFactory
     */
    LibraryBinary("LibraryBinary"),

    /**
     * A binary library with PSI files decompiled from the library's class files. Instead of building and indexing stubs (if applicable),
     * the test's declaration provider will instead index the decompiled PSI files directly.
     *
     * [LibraryBinaryDecompiled] should be specified when tests access the library's files as test files, usually as a main file in a main
     * module. See [AbstractAnalysisApiBasedTest][org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest] for an
     * overview of "main module" and "main file".
     *
     * @see org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtLibraryBinaryDecompiledTestModuleFactory
     */
    LibraryBinaryDecompiled("LibraryBinaryDecompiled"),

    /**
     * @see org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtLibrarySourceTestModuleFactory
     */
    LibrarySource("LibrarySource"),

    /**
     * @see org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtScriptTestModuleFactory
     */
    ScriptSource("ScriptSource"),

    /**
     * @see org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtCodeFragmentTestModuleFactory
     */
    CodeFragment("CodeFragment"),

    /**
     * This is currently only used in LL FIR tests and not supported by a test framework module factory.
     */
    NotUnderContentRoot("NotUnderContentRoot"),
}

val TestModule.explicitTestModuleKind: TestModuleKind?
    get() = directives.singleOrZeroValue(AnalysisApiTestDirectives.MODULE_KIND)
