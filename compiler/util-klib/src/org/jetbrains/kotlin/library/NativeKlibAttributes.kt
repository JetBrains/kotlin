/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library

/**
 * Indicates whether this [Klib] is from the Kotlin/Native distribution.
 */
// TODO (KT-81411): Move this attribute to a Native-related module.
var Klib.isFromKotlinNativeDistribution: Boolean by klibFlag()
    internal set
