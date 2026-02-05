/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.library.KotlinLibrary

/**
 * TODO (KT-61096): Consider extracting the base (abstract) class and making this one to be one of the implementations.
 *
 * Kotlin libraries (KLIBs) that were loaded from the file system for the use in Kotlin/JS or Kotlin/Wasm compiler CLI.
 *
 * @property all The full list of loaded [KotlinLibrary]s.
 *  This list consists of KLIBs who's paths were passed via CLI using options [K2JSCompilerArguments.libraries] and
 *  [K2JSCompilerArguments.includes]. The order of elements in the list preserves the order in which KLIBs were
 *  specified in CLI, with the exception for stdlib, which must go the first in the list. Included libraries go the last.
 *
 * @property friends Only KLIBs having status of "friends" ([K2JSCompilerArguments.friendModules] CLI option).
 *  Note: All [friends] are also included into [all].
 *
 * @property included Only the included KLIB ([K2JSCompilerArguments.includes] CLI option), if there was any.
 *  Note: [included] is also in [all].
 */
class LoadedKlibs(
    val all: List<KotlinLibrary>,
    val friends: List<KotlinLibrary> = emptyList(),
    val included: KotlinLibrary? = null
)

/**
 * TODO (KT-61096): Consider extracting the base (abstract) class and making this one to be one of the implementations.
 *
 * Kotlin libraries (KLIBs) that were loaded from the file system for the use in Kotlin/Native compiler CLI.
 *
 * @property all The full list of loaded [KotlinLibrary]s.
 *  This list consists of KLIBs who's paths were passed via CLI using options [K2NativeCompilerArguments.libraries] and
 *  [K2NativeCompilerArguments.includes]. The order of elements in the list preserves the order in which KLIBs were
 *  specified in CLI, with the exception for stdlib, which must go the first in the list. Included libraries go the last.
 *
 * @property friends Only KLIBs having status of "friends" ([K2NativeCompilerArguments.friendModules] CLI option).
 *  Note: All [friends] are also included into [all].
 *
 * @property included Only the included KLIB ([K2NativeCompilerArguments.includes] CLI option), if there were any.
 *  Note: [included] is also in [all].
 */
class LoadedNativeKlibs(
    val all: List<KotlinLibrary>,
    val friends: List<KotlinLibrary> = emptyList(),
    val included: List<KotlinLibrary> = emptyList(),
)
