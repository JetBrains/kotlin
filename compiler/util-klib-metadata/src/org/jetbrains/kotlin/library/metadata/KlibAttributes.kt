/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.metadata

import org.jetbrains.kotlin.library.Klib
import org.jetbrains.kotlin.library.klibFlag

/**
 * Indicates whether there are declarations in the [Klib] that have been accessed during the frontend resolve phase.
 *
 * This flag is used as an optimization in Kotlin/Native: There might be 100+ platform libraries in the Kotlin/Native distribution,
 * all are added to the "classpath" implicitly. We don't want all these libraries to participate in the expensive IR-linkage
 * process on the 2nd compilation stage. And using this flag, we can skip those that are not needed.
 */
var Klib.hasDeclarationsAccessedDuringFrontendResolve: Boolean by klibFlag()
    internal set
