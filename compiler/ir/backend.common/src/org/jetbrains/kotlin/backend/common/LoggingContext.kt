/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.backend.common.phaser.CompilerPhase

interface LoggingContext {

    /**
     * Whether logging is enabled for the current [CompilerPhase] by the `-Xverbose-phases` CLI option.
     */
    var inVerbosePhase: Boolean

    /**
     * If [inVerbosePhase] is `true`, prints [message] to the standard error stream.
     */
    fun log(message: () -> String) {
        if (inVerbosePhase) {
            System.err.println(message())
        }
    }
}
