/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmGeneratorExtensions
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.isSubtypeOfClass
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.ir.util.coerceToUnitIfNeeded
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.psi2ir.transformations.InsertImplicitCasts

internal val jvmCoercionToUnitPhase = makeIrFilePhase(
    ::JvmCoercionToUnitPatcher,
    name = "JvmCoercionToUnit",
    description = "Insert conversions to unit after IrCalls where needed"
)

class JvmCoercionToUnitPatcher(val context: JvmBackendContext) :
    InsertImplicitCasts(
        context.builtIns, context.irBuiltIns,
        TypeTranslator(context.ir.symbols.externalSymbolTable, context.state.languageVersionSettings),
        JvmGeneratorExtensions.samConversion
    ),
    FileLoweringPass {

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(this)
    }

    override fun IrExpression.coerceToUnit(): IrExpression {
        if (type.isSubtypeOfClass(context.irBuiltIns.unitClass) && this is IrCall) {
            return coerceToUnitIfNeeded(symbol.owner.returnType.toKotlinType(), context.irBuiltIns)
        }

        return this
    }
}
