/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.library

import org.jetbrains.kotlin.library.Klib
import org.jetbrains.kotlin.library.klibFlag

/**
 * Indicates whether this [Klib] is implicitly loaded from the Kotlin/Native distribution.
 *
 * Note: "Implicitly" means that the user has not explicitly specified this library in compiler's CLI
 * arguments such as `-library` or `-Xinclude`.
 */
// TODO (KT-61096): Move this attribute to a Native-related module.
var Klib.isImplicitlyLoadedFromKotlinNativeDistribution: Boolean by klibFlag()
    // TODO (KT-61096): After moving, make the setter to be internal.
    set

/**
 * Indicates whether this [Klib] is explicitly specified by the user in compiler's CLI arguments.
 * The opposite to [isImplicitlyLoadedFromKotlinNativeDistribution].
 */
val Klib.isExplicitlySpecifiedByUserInCLIArgument: Boolean
    get() = !isImplicitlyLoadedFromKotlinNativeDistribution
