/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.diagnostics

/**
 * The versions involved in loading a class compiled by an incompatible version of the compiler.
 *
 * @property actualVersion The metadata version of the class.
 * @property compilerVersion The metadata version of the current compiler.
 * @property languageVersion The metadata version which corresponds to the current language version.
 * @property expectedVersion The metadata version the class is expected to have.
 * @property filePath The path to the file the class is loaded from.
 */
@KaUnstableDiagnosticApi
public class KaIncompatibleVersionErrorData(
    public val actualVersion: String,
    public val compilerVersion: String,
    public val languageVersion: String,
    public val expectedVersion: String,
    public val filePath: String,
)
