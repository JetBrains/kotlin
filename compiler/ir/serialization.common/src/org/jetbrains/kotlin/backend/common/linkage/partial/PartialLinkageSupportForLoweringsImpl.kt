/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.linkage.partial

import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.linkage.partial.PartialLinkageCase
import org.jetbrains.kotlin.ir.linkage.partial.PartialLinkageSupportForLowerings
import org.jetbrains.kotlin.ir.linkage.partial.PartiallyLinkedDeclarationOrigin
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.ir.linkage.partial.PartialLinkageUtils.File as PLFile

fun createPartialLinkageSupportForLowerings(isEnabled: Boolean, builtIns: IrBuiltIns, messageLogger: IrMessageLogger) =
    if (isEnabled) PartialLinkageSupportForLoweringsImpl(builtIns, messageLogger) else PartialLinkageSupportForLowerings.DISABLED

internal class PartialLinkageSupportForLoweringsImpl(
    private val builtIns: IrBuiltIns,
    private val messageLogger: IrMessageLogger
) : PartialLinkageSupportForLowerings {
    override val isEnabled get() = true

    override fun throwLinkageError(
        partialLinkageCase: PartialLinkageCase,
        element: IrElement,
        file: PLFile,
        suppressWarningInCompilerOutput: Boolean
    ): IrCall {
        val errorMessage = if (suppressWarningInCompilerOutput)
            partialLinkageCase.renderLinkageError()
        else
            renderAndLogLinkageError(partialLinkageCase, element, file)

        return IrCallImpl(
            startOffset = element.startOffset,
            endOffset = element.endOffset,
            type = builtIns.nothingType,
            symbol = builtIns.linkageErrorSymbol,
            typeArgumentsCount = 0,
            valueArgumentsCount = 1,
            origin = IrStatementOrigin.PARTIAL_LINKAGE_RUNTIME_ERROR
        ).apply {
            putValueArgument(0, IrConstImpl.string(startOffset, endOffset, builtIns.stringType, errorMessage))
        }
    }

    fun renderAndLogLinkageError(partialLinkageCase: PartialLinkageCase, element: IrElement, file: PLFile): String {
        val errorMessage = partialLinkageCase.renderLinkageError()
        val locationInSourceCode = file.computeLocationForOffset(element.startOffsetOfFirstDenotableIrElement())

        messageLogger.report(IrMessageLogger.Severity.WARNING, errorMessage, locationInSourceCode) // It's OK. We log it as a warning.

        return errorMessage
    }

    companion object {
        private tailrec fun IrElement.startOffsetOfFirstDenotableIrElement(): Int = when (this) {
            is IrPackageFragment -> UNDEFINED_OFFSET
            !is IrDeclaration -> {
                // We don't generate non-denotable IR expressions in the course of partial linkage.
                startOffset
            }

            else -> if (origin is PartiallyLinkedDeclarationOrigin) {
                // There is no sense to take coordinates from the declaration that does not exist in the code.
                // Let's take the coordinates of the parent.
                parent.startOffsetOfFirstDenotableIrElement()
            } else {
                startOffset
            }
        }
    }
}
