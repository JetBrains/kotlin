/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.builders.v2

import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrReturnTargetSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.name.Name

class IrBuilderNew(
    val startOffset: Int,
    val endOffset: Int,
)

interface IrBuiltInsAware {
    val irBuiltIns: IrBuiltIns
}

class StatementList {
    val statements = mutableListOf<IrStatement>()
    fun resultType(irBuiltIns: IrBuiltIns) : IrType {
        return (statements.lastOrNull() as? IrExpression)?.type ?: irBuiltIns.unitType
    }
}


class DeclarationParentScope(
    val parent: IrDeclarationParent,
) {
    private var lastTemporaryIndex: Int = 0

    fun inventNameForTemporary(prefix: String, suffix: String? = null): Name {
        val index = lastTemporaryIndex++
        return Name.identifier(if (suffix != null) "$prefix${index}_$suffix" else "$prefix$index")
    }
}

context(list: StatementList)
operator fun Iterable<IrStatement>.unaryPlus() { list.statements.addAll(this) }
context(list: StatementList)
operator fun IrStatement.unaryPlus() { list.statements.add(this) }

val UndefinedOffsetBuilder = IrBuilderNew(UNDEFINED_OFFSET, UNDEFINED_OFFSET)
val SyntheticOffsetBuilder = IrBuilderNew(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET)

fun IrElement.builderAt() = IrBuilderNew(startOffset, endOffset)
inline fun <T: IrElement?> buildIrAt(element: IrElement, body: IrBuilderNew.() -> T) =
    element.builderAt().body()

fun IrBuilderNew.withParent(parent: IrFunction, block: context(DeclarationParentScope, IrReturnTargetSymbol) IrBuilderNew.() -> Unit) =
    block(DeclarationParentScope(parent), parent.symbol, this)

fun IrBuilderNew.withParent(parent: IrDeclarationParent, block: context(DeclarationParentScope) IrBuilderNew.() -> Unit) =
    block(DeclarationParentScope(parent), this)