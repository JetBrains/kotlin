/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

/**
 * VArray<Mfvc> flattening scheme
 *
 * See [explanation](https://github.com/grechkovlad/vArrayBenchmarksReport) what do these schemes mean
 */
enum class JvmMfvcVArrayFlatteningScheme {
    PER_TYPE, PER_SIZE, THREE_ARRAYS, TWO_ARRAYS
}