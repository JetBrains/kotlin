/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.builders.v2

import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrStatementContainer
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.isImmutable
import org.jetbrains.kotlin.name.Name

context(scope: DeclarationParentScope)
fun IrBuilderNew.irVariable(
    name: Name,
    initializer: IrExpression,
    origin: IrDeclarationOrigin,
    type: IrType = initializer.type,
    isMutable: Boolean = false,
    isConst: Boolean = false,
    isLateinit: Boolean = false,
): IrVariable {
    return irVariable(name, type, origin, isMutable, isConst, isLateinit)
        .apply { this.initializer = initializer }
}

context(scope: DeclarationParentScope)
fun IrBuilderNew.irVariable(
    name: Name,
    type: IrType,
    origin: IrDeclarationOrigin,
    isMutable: Boolean = false,
    isConst: Boolean = false,
    isLateinit: Boolean = false,
): IrVariable {
    return IrVariableImpl(
        startOffset, endOffset,
        origin,
        IrVariableSymbolImpl(),
        name,
        type,
        isVar = isMutable,
        isConst = isConst,
        isLateinit = isLateinit
    ).apply {
        this.parent = scope.parent
    }
}

context(scope: DeclarationParentScope)
fun IrBuilderNew.irTemporary(
    type: IrType,
    suffix: String? = null,
    prefix: String = "tmp",
    isMutable: Boolean = false,
    isConst: Boolean = false,
    isLateinit: Boolean = false,
) : IrVariable {
    return irVariable(
        name = scope.inventNameForTemporary(prefix, suffix),
        type = type,
        origin = IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
        isMutable = isMutable,
        isConst = isConst,
        isLateinit = isLateinit,
    )
}

context(scope: DeclarationParentScope)
fun IrBuilderNew.irTemporary(
    value: IrExpression,
    type: IrType = value.type,
    suffix: String? = null,
    prefix: String = "tmp",
    isMutable: Boolean = false,
    isConst: Boolean = false,
    isLateinit: Boolean = false,
) : IrVariable {
    return irTemporary(
        type = type,
        suffix = suffix,
        prefix = prefix,
        isMutable = isMutable,
        isConst = isConst,
        isLateinit = isLateinit,
    ).apply { this.initializer = value }
}

context(context: IrBuiltInsAware, scope: DeclarationParentScope)
inline fun IrBuilderNew.withTemporaryVarIfNecessary(
    value: IrExpression,
    body: (IrValueSymbol) -> IrExpression
): IrExpression {
    return if (value is IrGetValue && value.symbol.owner.isImmutable) {
        body(value.symbol)
    } else {
        irComposite {
            val t = irTemporary(value)
            +t
            val r = body(t.symbol)
            if (r is IrStatementContainer) {
                +r.statements
            } else {
                +r
            }
        }
    }
}