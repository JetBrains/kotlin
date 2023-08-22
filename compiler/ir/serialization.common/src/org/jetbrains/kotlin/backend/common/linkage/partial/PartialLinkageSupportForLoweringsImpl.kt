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
import org.jetbrains.kotlin.ir.linkage.partial.*
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.ir.linkage.partial.PartialLinkageUtils.File as PLFile

fun createPartialLinkageSupportForLowerings(
    partialLinkageConfig: PartialLinkageConfig,
    builtIns: IrBuiltIns,
    messageLogger: IrMessageLogger
): PartialLinkageSupportForLowerings = if (partialLinkageConfig.isEnabled)
    PartialLinkageSupportForLoweringsImpl(builtIns, PartialLinkageLogger(messageLogger, partialLinkageConfig.logLevel))
else
    PartialLinkageSupportForLowerings.DISABLED

internal class PartialLinkageSupportForLoweringsImpl(
    private val builtIns: IrBuiltIns,
    private val logger: PartialLinkageLogger
) : PartialLinkageSupportForLowerings {
    override val isEnabled get() = true

    /** To track the amount of rendered linkage issues. */
    var linkageIssuesRendered = 0
        private set

    /**
     * To track the amount of logged linkage issues.
     * Note that the following condition is always true: [linkageIssuesLogged] <= [linkageIssuesRendered].
     */
    var linkageIssuesLogged = 0
        private set

    /**
     * To track the amount of generated `throw` expressions.
     * Note that the following condition is always true: [throwExpressionsGenerated] <= [linkageIssuesRendered].
     */
    var throwExpressionsGenerated = 0
        private set

    override fun throwLinkageError(
        partialLinkageCase: PartialLinkageCase,
        element: IrElement,
        file: PLFile,
        doNotLog: Boolean
    ): IrCall {
        val errorMessage = if (doNotLog)
            renderLinkageError(partialLinkageCase) // Just render a message.
        else
            renderAndLogLinkageError(partialLinkageCase, element, file) // Render + log with the appropriate severity.

        throwExpressionsGenerated++ // Track each generated `throw` expression.

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
        val errorMessage = renderLinkageError(partialLinkageCase)
        val locationInSourceCode = file.computeLocationForOffset(element.startOffsetOfFirstDenotableIrElement())

        linkageIssuesLogged++ // Track each logged linkage issue.
        logger.log(errorMessage, locationInSourceCode)

        return errorMessage
    }

    private fun renderLinkageError(partialLinkageCase: PartialLinkageCase): String {
        linkageIssuesRendered++ // Track each rendered linkage issue.
        return partialLinkageCase.renderLinkageError()
    }

    companion object {
        private tailrec fun IrElement.startOffsetOfFirstDenotableIrElement(): Int = when (this) {
            is IrPackageFragment -> UNDEFINED_OFFSET
            !is IrDeclaration -> {
                // We don't generate non-denotable IR expressions in the course of partial linkage.
                startOffset
            }

            else -> if (origin in PartiallyLinkedDeclarationOrigin.entries) {
                // There is no sense to take coordinates from the declaration that does not exist in the code.
                // Let's take the coordinates of the parent.
                parent.startOffsetOfFirstDenotableIrElement()
            } else {
                startOffset
            }
        }
    }
}
