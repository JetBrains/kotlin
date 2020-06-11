/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.backend.common.interpreter.EvaluationMode
import org.jetbrains.kotlin.backend.common.interpreter.IrCompileTimeChecker
import org.jetbrains.kotlin.backend.common.interpreter.IrInterpreter
import org.jetbrains.kotlin.backend.common.interpreter.toIrConst
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrErrorExpression
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isArray
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

class IrConstTransformer(irModuleFragment: IrModuleFragment) : IrElementTransformerVoid() {
    private val interpreter = IrInterpreter(irModuleFragment)

    private fun IrExpression.report(original: IrExpression): IrExpression {
        if (this == original) return this
        return if (this !is IrErrorExpression) this else original
    }

    override fun visitCall(expression: IrCall): IrExpression {
        if (expression.accept(IrCompileTimeChecker(mode = EvaluationMode.ONLY_BUILTINS), null)) {
            return interpreter.interpret(expression).report(expression)
        }
        return super.visitCall(expression)
    }

    override fun visitField(declaration: IrField): IrStatement {
        val initializer = declaration.initializer
        val expression = initializer?.expression ?: return declaration
        if (expression is IrConst<*>) return declaration
        val isCompileTimeComputable = expression.accept(IrCompileTimeChecker(declaration, mode = EvaluationMode.ONLY_BUILTINS), null)
        val isConst = declaration.correspondingPropertySymbol?.owner?.isConst == true
        if (isConst && !isCompileTimeComputable) {
            //throw AssertionError("Const property is used only with functions annotated as CompileTimeCalculation: " + declaration.dump())
        } else if (isCompileTimeComputable) {
            initializer.expression = interpreter.interpret(expression).report(expression)
        }
        return declaration
    }

    override fun visitClass(declaration: IrClass): IrStatement {
        declaration.annotations.forEach {
            for (i in 0 until it.valueArgumentsCount) {
                val arg = it.getValueArgument(i) ?: continue
                if (arg.accept(IrCompileTimeChecker(mode = EvaluationMode.ONLY_BUILTINS), null)) {
                    val const = interpreter.interpret(arg).report(arg)
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