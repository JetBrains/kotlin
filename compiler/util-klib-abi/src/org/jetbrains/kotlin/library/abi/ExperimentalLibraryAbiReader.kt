/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.abi

/**
 * This marker distinguishes the experimental Kotlin library ABI reader API and is used to opt-in.
 *
 * Any usage of a declaration annotated with `@ExperimentalLibraryAbiReader` must be accepted either by
 * annotating that usage with the [OptIn] annotation, e.g. `@OptIn(ExperimentalLibraryAbiReader::class)`,
 * or by using the compiler argument `-opt-in=org.jetbrains.kotlin.library.abi.ExperimentalLibraryAbiReader`.
 */
@RequiresOptIn
annotation class ExperimentalLibraryAbiReader
