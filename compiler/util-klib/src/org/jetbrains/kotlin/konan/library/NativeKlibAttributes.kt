/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.library

import org.jetbrains.kotlin.library.Klib
import org.jetbrains.kotlin.library.klibFlag
import org.jetbrains.kotlin.library.loader.KlibLoader

/**
 * Indicates whether this [Klib] belongs to the Kotlin/Native distribution.
 */
// TODO (KT-61096): Move this attribute to a Native-related module.
var Klib.isFromKotlinNativeDistribution: Boolean by klibFlag()
    // TODO (KT-61096): After moving, make the setter to be internal.
    set

/**
 * Indicates whether this [Klib] was implicitly loaded from the Kotlin/Native distribution.
 *
 * Notes:
 * - "Implicitly" means that the user has not specified this library in compiler's CLI
 *   arguments such as `-library` or `-Xinclude`. That were [KlibLoader] and [KlibNativeDistributionLibraryProvider]
 *   who decided to load this library from the Kotlin/Native distribution.
 * - `isImplicitlyLoadedFromKotlinNativeDistribution == true` assumes that `isFromKotlinNativeDistribution == true`,
 *    but not vice versa.
 */
// TODO (KT-61096): Move this attribute to a Native-related module.
var Klib.isImplicitlyLoadedFromKotlinNativeDistribution: Boolean by klibFlag()
    // TODO (KT-61096): After moving, make the setter to be internal.
    set

/**
 * Indicates whether this [Klib] is explicitly specified by the user in compiler's CLI arguments.
 * The opposite to [isImplicitlyLoadedFromKotlinNativeDistribution].
 */
// TODO (KT-61096): Move this attribute to a Native-related module.
val Klib.isExplicitlySpecifiedByUserInCLIArgument: Boolean
    get() = !isImplicitlyLoadedFromKotlinNativeDistribution
