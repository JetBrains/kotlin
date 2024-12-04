/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.checkers.context

import org.jetbrains.kotlin.ir.IrElement

/**
 * Manages temporary updates to a [CheckerContext] during the processing of an [IrElement].
 */
internal interface ContextUpdater {
    /**
     * Temporarily updates the [context]  based on the given [element], executes [block] within the updated context,
     * and then restores the context to its original state.
     */
    fun runInNewContext(context: CheckerContext, element: IrElement, block: () -> Unit)
}
