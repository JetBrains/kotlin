/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.library.KotlinLibrary

/**
 * Kotlin libraries (KLIBs) that were loaded from the file system.
 *
 * @property all The full list of loaded [KotlinLibrary]s.
 *  This list consists of KLIBs who's paths were passed via CLI using options `-library` (Kotlin/Native), `-libraries` (Kotlin/JS),
 *  and `-Xinclude` (both). The order of elements in the list preserves the order in which KLIBs were specified in CLI, with the
 *  exception for stdlib, which must go the first in the list. Included libraries go the last.
 *
 * @property friends Only KLIBs having status of "friends" (`-friend-modules` Kotlin/Native CLI option or `-Xfriend-modules`
 *  Kotlin/JS CLI option). Note: All [friends] are also included into [all].
 *
 * @property included Only the included KLIB (`-Xinclude` CLI option), if there was any.
 *  Note: [included] is also in [all].
 */
class LoadedKlibs(
    val all: List<KotlinLibrary>,
    val friends: List<KotlinLibrary> = emptyList(),
    val included: KotlinLibrary? = null
)
