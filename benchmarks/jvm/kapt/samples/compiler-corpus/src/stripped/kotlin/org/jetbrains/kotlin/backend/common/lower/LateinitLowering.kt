/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.ir.PreSerializationSymbols
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import org.jetbrains.kotlin.ir.types.isPrimitiveType
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.utils.atMostOne

open class LateinitLowering(
    private val loweringContext: LoweringContext,
    private val uninitializedPropertyAccessExceptionThrower: UninitializedPropertyAccessExceptionThrower,
) : FileLoweringPass, IrElementTransformerVoid() {
    private val visitedLateinitVariables = mutableSetOf<IrVariable>()

    constructor(loweringContext: LoweringContext) :
            this(loweringContext, UninitializedPropertyAccessExceptionThrower(loweringContext.symbols))

    override fun lower(irFile: IrFile) { TODO() }

    fun lower(irBody: IrBody) { TODO() }

    override fun visitProperty(declaration: IrProperty): IrStatement { TODO() }

    override fun visitVariable(declaration: IrVariable): IrStatement { TODO() }

    override fun visitGetValue(expression: IrGetValue): IrExpression { TODO() }

    override fun visitGetField(expression: IrGetField): IrExpression { TODO() }

    private fun IrField.isLateinitBackingField(): Boolean { TODO() }

    private fun IrProperty.isRealLateinit() =
        isLateinit && !isFakeOverride

    override fun visitCall(expression: IrCall): IrExpression { TODO() }

    protected open fun transformLateinitBackingField(backingField: IrField, property: IrProperty) { TODO() }

    private fun transformGetter(backingField: IrField, getter: IrFunction) { TODO() }

    private fun IrBuilderWithScope.throwUninitializedPropertyAccessException(name: String) =
        uninitializedPropertyAccessExceptionThrower.build(this, name)

    private fun isLateinitIsInitializedPropertyGetter(symbol: IrFunctionSymbol): Boolean = TODO()

}

private inline fun IrExpression.replaceTailExpression(crossinline transform: (IrExpression) -> IrExpression): IrExpression {
    var current = this
    var block: IrContainerExpression? = null
    while (current is IrContainerExpression) {
        block = current
        current = current.statements.last() as IrExpression
    }
    current = transform(current)
    if (block == null) {
        return current
    }
    block.statements[block.statements.size - 1] = current
    return this
}

open class UninitializedPropertyAccessExceptionThrower(private val symbols: PreSerializationSymbols) {
    open fun build(builder: IrBuilderWithScope, name: String): IrExpression { TODO() }
}
