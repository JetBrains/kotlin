/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

/**
 * The list of all possible test phase labels one can use in
 * [RUN_PIPELINE_TILL][org.jetbrains.kotlin.test.directives.TestPhaseDirectives.RUN_PIPELINE_TILL].
 */
enum class TestPhase {
    FRONTEND,
    FIR2IR,
    BACKEND;
}
