/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.evaluate

import org.jetbrains.kotlin.ir.interpreter.EvaluationMode
import org.jetbrains.kotlin.ir.interpreter.IrCompileTimeChecker
import org.jetbrains.kotlin.ir.interpreter.IrInterpreter
import org.jetbrains.kotlin.ir.interpreter.toIrConst
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

fun evaluateConstants(irModuleFragment: IrModuleFragment) {
    irModuleFragment.files.forEach { it.transformChildren(IrConstTransformer(irModuleFragment), null) }
}

//TODO create abstract class that will be common for this and lowering
class IrConstTransformer(irModuleFragment: IrModuleFragment) : IrElementTransformerVoid() {
    private val interpreter = IrInterpreter(irModuleFragment)

    private fun IrExpression.replaceIfError(original: IrExpression): IrExpression {
        return if (this !is IrErrorExpression) this else original
    }

    override fun visitCall(expression: IrCall): IrExpression {
        if (expression.accept(IrCompileTimeChecker(mode = EvaluationMode.ONLY_BUILTINS), null)) {
            return interpreter.interpret(expression).replaceIfError(expression)
        }
        return super.visitCall(expression)
    }

    override fun visitField(declaration: IrField): IrStatement {
        val initializer = declaration.initializer
        val expression = initializer?.expression ?: return declaration
        if (expression is IrConst<*>) return declaration
        val isCompileTimeComputable = expression.accept(IrCompileTimeChecker(declaration, mode = EvaluationMode.ONLY_BUILTINS), null)
        val isConst = declaration.correspondingPropertySymbol?.owner?.isConst == true
        if (isConst && isCompileTimeComputable) {
            initializer.expression = interpreter.interpret(expression).replaceIfError(expression)
        }

        return declaration
    }

    override fun visitClass(declaration: IrClass): IrStatement {
        declaration.annotations.forEach {
            for (i in 0 until it.valueArgumentsCount) {
                val arg = it.getValueArgument(i) ?: continue
                if (arg.accept(IrCompileTimeChecker(mode = EvaluationMode.ONLY_BUILTINS), null)) {
                    val const = interpreter.interpret(arg).replaceIfError(arg)
                    it.putValueArgument(i, const.convertToConstIfPossible(it.symbol.owner.valueParameters[i].type))
                }
            }
        }
        return super.visitClass(declaration)
    }

    private fun IrExpression.convertToConstIfPossible(type: IrType): IrExpression {
        if (this !is IrConst<*>) return this
        if (type.isArray()) return this.convertToConstIfPossible((type as IrSimpleType).arguments.single().typeOrNull!!)
        return this.value.toIrConst(type, this.startOffset, this.endOffset)
    }
}