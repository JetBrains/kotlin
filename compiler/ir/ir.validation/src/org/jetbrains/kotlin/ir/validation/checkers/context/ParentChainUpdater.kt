/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.validation.checkers.context

import org.jetbrains.kotlin.ir.IrElement

object ParentChainUpdater : ContextUpdater {
    override fun onEnterElement(context: CheckerContext, element: IrElement) {
        context.parentChain.add(element)
    }

    override fun onExitElement(context: CheckerContext, element: IrElement) {
        context.parentChain.removeLast()
    }
}