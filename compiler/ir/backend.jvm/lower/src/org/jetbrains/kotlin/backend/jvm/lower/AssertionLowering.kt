/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.ir.asInlinable
import org.jetbrains.kotlin.backend.common.ir.inline
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.buildAssertionsDisabledField
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.JVMAssertionsMode
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer

internal val assertionPhase = makeIrFilePhase(
    ::AssertionLowering,
    name = "Assertion",
    description = "Lower assert calls depending on the assertions mode",
    // Necessary to place the `$assertionsDisabled` field into the reference's class, not the
    // class that contains it.
    prerequisite = setOf(functionReferencePhase)
)

private class AssertionLowering(private val context: JvmBackendContext) :
    FileLoweringPass,
    IrElementTransformer<AssertionLowering.ClassInfo?>
{
    // Keeps track of the $assertionsDisabled field, which we generate lazily for classes containing
    // assertions when compiled with -Xassertions=jvm.
    class ClassInfo(val irClass: IrClass, val topLevelClass: IrClass, var assertionsDisabledField: IrField? = null)

    private val scopeOwnerStack = java.util.ArrayDeque<IrDeclaration>()

    override fun lower(irFile: IrFile) {
        // In legacy mode we treat assertions as inline function calls
        if (context.state.assertionsMode != JVMAssertionsMode.LEGACY)
            irFile.transformChildren(this, null)
    }

    override fun visitDeclaration(declaration: IrDeclarationBase, data: ClassInfo?): IrStatement {
        scopeOwnerStack.push(declaration)
        val result = super.visitDeclaration(declaration, data)
        scopeOwnerStack.pop()
        return result
    }

    override fun visitClass(declaration: IrClass, data: ClassInfo?): IrStatement {
        val info = ClassInfo(declaration, data?.topLevelClass ?: declaration)

        visitDeclaration(declaration, info)

        // Note that it's necessary to add this member at the beginning of the class, before all user-visible
        // initializers, which may contain assertions. At the same time, assertions are supposed to be enabled
        // for code which runs before class initialization. This is the reason why this field records whether
        // assertions are disabled rather than enabled. During initialization, $assertionsDisabled is going
        // to be false, meaning that assertions are checked.
        info.assertionsDisabledField?.let {
            declaration.declarations.add(0, it)
        }

        return declaration
    }

    override fun visitCall(expression: IrCall, data: ClassInfo?): IrElement {
        val function = expression.symbol.owner
        if (!function.isAssert)
            return super.visitCall(expression, data)

        val mode = context.state.assertionsMode
        if (mode == JVMAssertionsMode.ALWAYS_DISABLE)
            return IrCompositeImpl(expression.startOffset, expression.endOffset, context.irBuiltIns.unitType)

        context.createIrBuilder(scopeOwnerStack.peek().symbol).run {
            at(expression)
            val assertCondition = expression.getValueArgument(0)!!
            val lambdaArgument = if (function.valueParameters.size == 2) expression.getValueArgument(1) else null

            return if (mode == JVMAssertionsMode.ALWAYS_ENABLE) {
                checkAssertion(assertCondition, lambdaArgument)
            } else {
                require(mode == JVMAssertionsMode.JVM && data != null)
                irIfThen(
                    irNot(getAssertionDisabled(this, data)),
                    checkAssertion(assertCondition, lambdaArgument)
                )
            }
        }
    }

    private fun IrBuilderWithScope.checkAssertion(assertCondition: IrExpression, lambdaArgument: IrExpression?) =
        irBlock {
            val generator = lambdaArgument?.asInlinable(this)
            val constructor = this@AssertionLowering.context.ir.symbols.assertionErrorConstructor
            val throwError = irThrow(irCall(constructor).apply {
                putValueArgument(0, generator?.inline(parent) ?: irString("Assertion failed"))
            })
            +irIfThen(irNot(assertCondition), throwError)
        }

    private fun getAssertionDisabled(irBuilder: IrBuilderWithScope, data: ClassInfo): IrExpression {
        if (data.assertionsDisabledField == null)
            data.assertionsDisabledField = data.irClass.buildAssertionsDisabledField(context, data.topLevelClass)
        return irBuilder.irGetField(null, data.assertionsDisabledField!!)
    }

    private val IrFunction.isAssert: Boolean
        get() = name.asString() == "assert" && getPackageFragment().packageFqName == StandardNames.BUILT_INS_PACKAGE_FQ_NAME
}
