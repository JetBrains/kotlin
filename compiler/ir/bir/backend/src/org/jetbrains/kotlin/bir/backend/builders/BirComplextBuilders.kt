/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.backend.builders

import org.jetbrains.kotlin.bir.SourceSpan
import org.jetbrains.kotlin.bir.backend.BirBackendContext
import org.jetbrains.kotlin.bir.declarations.BirVariable
import org.jetbrains.kotlin.bir.declarations.impl.BirVariableImpl
import org.jetbrains.kotlin.bir.expressions.BirBranch
import org.jetbrains.kotlin.bir.expressions.BirCall
import org.jetbrains.kotlin.bir.expressions.BirExpression
import org.jetbrains.kotlin.bir.expressions.BirWhen
import org.jetbrains.kotlin.bir.expressions.impl.BirCallImpl
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.name.Name

context(BirBackendContext, BirStatementBuilderScope)
@OptIn(ObsoleteDescriptorBasedAPI::class)
inline fun birTemporaryVariable(
    type: BirType,
    isMutable: Boolean = false,
    nameHint: String? = null,
    addIndexToName: Boolean = true,
    origin: IrDeclarationOrigin = IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
    sourceSpan: SourceSpan = this@BirStatementBuilderScope.sourceSpan,
    block: BirVariable.() -> Unit = {},
): BirVariable {
    val name = Name.identifier(getNameForTemporary(nameHint, addIndexToName))
    return BirVariableImpl(
        sourceSpan = sourceSpan, origin = origin, name = name,
        type = type, isVar = isMutable, isConst = false, isLateinit = false,
    ).apply(block)
}

context(BirBackendContext, BirStatementBuilderScope)
inline fun birTemporaryVariable(
    initializer: BirExpression,
    type: BirType = initializer.type,
    isMutable: Boolean = false,
    origin: IrDeclarationOrigin = IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
    nameHint: String? = null,
    addIndexToName: Boolean = true,
    sourceSpan: SourceSpan = initializer.sourceSpan,
    block: BirVariable.() -> Unit = {},
): BirVariable {
    return birTemporaryVariable(type, isMutable, nameHint, addIndexToName, origin, sourceSpan) {
        this.initializer = initializer
        block()
    }
}


context(BirBackendContext, BirStatementBuilderScope)
inline fun birEquals(
    arg1: BirExpression,
    arg2: BirExpression,
    origin: IrStatementOrigin? = IrStatementOrigin.EQEQ,
    block: BirCall.() -> Unit = {},
): BirCall =
    BirCallImpl(
        sourceSpan = sourceSpan,
        type = birBuiltIns.booleanType,
        symbol = birBuiltIns.eqeqSymbol,
        dispatchReceiver = null,
        extensionReceiver = null,
        origin = origin,
        typeArguments = emptyList(),
        contextReceiversCount = 0,
        superQualifierSymbol = null
    ).apply {
        valueArguments += arg1
        valueArguments += arg2
        block()
    }

context(BirBackendContext, BirStatementBuilderScope)
inline fun birNotEquals(
    arg1: BirExpression,
    arg2: BirExpression,
    origin: IrStatementOrigin? = IrStatementOrigin.EXCLEQ,
    block: BirCall.() -> Unit = {},
): BirCall =
    BirCallImpl(
        sourceSpan = sourceSpan,
        type = birBuiltIns.booleanType,
        symbol = birBuiltIns.booleanNotSymbol,
        dispatchReceiver = birEquals(arg1, arg2, origin = IrStatementOrigin.EXCLEQ),
        extensionReceiver = null,
        origin = origin,
        typeArguments = emptyList(),
        contextReceiversCount = 0,
        superQualifierSymbol = null
    ).apply(block)


context(BirBackendContext, BirStatementBuilderScope)
inline fun birIfThenElse(
    type: BirType,
    condition: BirExpression,
    thenPart: BirExpression,
    elsePart: BirExpression,
    origin: IrStatementOrigin? = null,
    block: BirWhen.() -> Unit = {},
) = birWhen(type, origin) {
    branches += birBranch(condition, thenPart)
    branches += birElseBranch(elsePart)
}.apply(block)