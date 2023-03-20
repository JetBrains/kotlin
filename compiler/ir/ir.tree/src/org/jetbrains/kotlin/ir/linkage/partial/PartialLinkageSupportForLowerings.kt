/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.linkage.partial

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.linkage.partial.PartialLinkageUtils.File as PLFile

interface PartialLinkageSupportForLowerings {
    val isEnabled: Boolean

    fun throwLinkageError(
        partialLinkageCase: PartialLinkageCase,
        element: IrElement,
        file: PLFile,
        doNotLog: Boolean = false
    ): IrCall

    companion object {
        val DISABLED = object : PartialLinkageSupportForLowerings {
            override val isEnabled get() = false
            override fun throwLinkageError(
                partialLinkageCase: PartialLinkageCase,
                element: IrElement,
                file: PLFile,
                doNotLog: Boolean
            ): IrCall = error("Should not be called")
        }
    }
}
