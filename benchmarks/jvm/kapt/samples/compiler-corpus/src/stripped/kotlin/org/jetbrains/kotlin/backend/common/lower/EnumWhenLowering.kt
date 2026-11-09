/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.compilationException
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.getOrSetIfNull

/**
 * Optimization: replaces `when` subjects of enum types with their ordinals.
 */
open class EnumWhenLowering(protected open val context: CommonBackendContext) : IrElementTransformerVoidWithContext(), FileLoweringPass {

    protected open fun mapConstEnumEntry(entry: IrEnumEntry): Int { TODO() }

    protected open fun mapRuntimeEnumEntry(builder: IrBuilderWithScope, subject: IrExpression): IrExpression = TODO()

    override fun lower(irFile: IrFile) { TODO() }

    override fun visitBlock(expression: IrBlock): IrExpression { TODO() }

    private fun possibleToGenerateJumpTable(irWhen: IrWhen, subject: IrVariable): Boolean { TODO() }

    private fun getOther(lhs: IrExpression, rhs: IrExpression, subject: IrVariable): IrExpression? { TODO() }

    private fun transformBranches(
        irWhen: IrWhen,
        subject: IrVariable,
        subjectOrdinalProvider: Lazy<IrVariable>
    ): IrExpression { TODO() }

    private fun transformBranchSubexpression(
        irExpression: IrExpression,
        subject: IrVariable,
        subjectOrdinalProvider: Lazy<IrVariable>
    ): IrExpression = TODO()

    private fun transformEnumEquals(
        expression: IrCall,
        subject: IrVariable,
        subjectOrdinalProvider: Lazy<IrVariable>
    ): IrExpression { TODO() }

}

private var IrClass.enumEntriesMap: Map<Name, LoweredEnumEntryDescription>? by irAttribute(copyByDefault = false)

data class LoweredEnumEntryDescription(val ordinal: Int, val getterId: Int)

fun enumEntriesMap(enumClass: IrClass): Map<Name, LoweredEnumEntryDescription> { TODO() }
