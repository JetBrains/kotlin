/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.utils.isSubtypeOf
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.psi2ir.transformations.InsertImplicitCasts

class JvmCoercionToUnitPatcher(val context: JvmBackendContext) :
    InsertImplicitCasts(
        context.builtIns, context.irBuiltIns,
        TypeTranslator(context.ir.symbols.externalSymbolTable, context.state.languageVersionSettings)
    ),
    FileLoweringPass {

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(this)
    }

    override fun IrExpression.coerceToUnit(): IrExpression {
        if (type.isSubtypeOf(context.irBuiltIns.unitType) && this is IrCall) {
            return coerceToUnitIfNeeded(symbol.owner.returnType.toKotlinType())
        }

        return this
    }
}