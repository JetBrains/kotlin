/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators

import org.jetbrains.kotlin.generators.model.MethodModel

abstract class TestGenerator(
    methodGenerators: List<MethodGenerator<*>>
) {
    protected val methodGenerators: Map<MethodModel.Kind, MethodGenerator<*>> =
        methodGenerators.associateBy { it.kind }.withDefault { error("Generator for method with kind $it not found") }

    abstract fun generateAndSave(testClass: TestGroup.TestClass, dryRun: Boolean, mainClassName: String?): GenerationResult

    data class GenerationResult(val newFileGenerated: Boolean, val testSourceFilePath: String)
}

