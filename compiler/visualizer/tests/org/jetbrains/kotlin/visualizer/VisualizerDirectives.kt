/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.visualizer

import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer

internal object VisualizerDirectives : SimpleDirectivesContainer() {
    val TEST_FILE_PATH by stringDirective(
        description = "Specify that part of test file path must be replaced with EXPECTED_FILE_PATH"
    )
    val EXPECTED_FILE_PATH by stringDirective(
        description = "Specify the path to expected result file that will be inserted instead of TEST_FILE_PATH"
    )
}