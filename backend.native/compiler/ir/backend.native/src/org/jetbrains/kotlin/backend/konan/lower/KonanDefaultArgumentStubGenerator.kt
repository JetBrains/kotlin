/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.lower.DefaultArgumentStubGenerator
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrExpression

internal class KonanDefaultArgumentStubGenerator(override val context: Context)
    : DefaultArgumentStubGenerator(context, skipInlineMethods = false)
{
    override fun IrBlockBodyBuilder.selectArgumentOrDefault(
            defaultFlag: IrExpression,
            parameter: IrValueParameter,
            default: IrExpression
    ): IrValueDeclaration {
        val value = irIfThenElse(parameter.type, irNotEquals(defaultFlag, irInt(0)), default, irGet(parameter))
        return createTmpVariable(value, nameHint = parameter.name.asString())
    }
}