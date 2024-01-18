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
 * [KtModule][org.jetbrains.kotlin.analysis.project.structure.KtModule]s used in the test. This essentially defines the context in which a
 * test file is analyzed.
 *
 * The test module kind can also be overridden for a specific test module in multi-module tests using the
 * [MODULE_KIND][org.jetbrains.kotlin.analysis.test.framework.AnalysisApiTestDirectives.MODULE_KIND] directive. This allows e.g. source
 * module tests to refer to binary library dependencies.
 */
enum class TestModuleKind(val suffix: String) {
    /**
     * @see org.jetbrains.kotlin.analysis.test.framework.project.structure.KtSourceModuleFactory
     */
    Source("Source"),

    /**
     * @see org.jetbrains.kotlin.analysis.test.framework.project.structure.KtLibraryBinaryModuleFactory
     */
    LibraryBinary("LibraryBinary"),

    /**
     * @see org.jetbrains.kotlin.analysis.test.framework.project.structure.KtLibrarySourceModuleFactory
     */
    LibrarySource("LibrarySource"),

    /**
     * @see org.jetbrains.kotlin.analysis.test.framework.project.structure.KtScriptModuleFactory
     */
    ScriptSource("ScriptSource"),

    /**
     * @see org.jetbrains.kotlin.analysis.test.framework.project.structure.KtCodeFragmentModuleFactory
     */
    CodeFragment("CodeFragment")
}

val TestModule.moduleKind: TestModuleKind?
    get() = directives.singleOrZeroValue(AnalysisApiTestDirectives.MODULE_KIND)
