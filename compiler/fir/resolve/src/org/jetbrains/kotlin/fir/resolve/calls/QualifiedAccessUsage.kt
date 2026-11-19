/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

enum class QualifiedAccessUsage(val isUsedAsReceiver: Boolean, val isUsedAsGetClassReceiver: Boolean) {
    /** Not a receiver: an argument, an operand, a statement of its own, and so on */
    STANDALONE(isUsedAsReceiver = false, isUsedAsGetClassReceiver = false),

    /** The explicit receiver of a call or of another qualified access */
    RECEIVER(isUsedAsReceiver = true, isUsedAsGetClassReceiver = false),

    /** The receiver of `::class` */
    GET_CLASS_RECEIVER(isUsedAsReceiver = true, isUsedAsGetClassReceiver = true),
}
