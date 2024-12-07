/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.checkers.context

import org.jetbrains.kotlin.backend.common.temporarilyPushing
import org.jetbrains.kotlin.ir.IrElement

internal object ParentChainUpdater : ContextUpdater {
    override fun runInNewContext(
        context: CheckerContext,
        element: IrElement,
        block: () -> Unit,
    ) {
        context.parentChain.temporarilyPushing(element) {
            block()
        }
    }
}