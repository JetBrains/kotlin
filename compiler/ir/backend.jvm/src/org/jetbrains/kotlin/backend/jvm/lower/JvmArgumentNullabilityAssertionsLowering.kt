/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.load.java.BuiltinMethodsWithSpecialGenericSignature

val jvmArgumentNullabilityAssertions =
    makeIrFilePhase(
        ::JvmArgumentNullabilityAssertionsLowering,
        name = "Nullability assertions on arguments",
        description = "Transform nullability assertions on arguments according to the compiler settings"
    )

class JvmArgumentNullabilityAssertionsLowering(context: JvmBackendContext) :
    FileLoweringPass, IrElementVisitorVoid {

    private val isWithUnifiedNullChecks =
        context.state.languageVersionSettings.apiVersion >= ApiVersion.KOTLIN_1_4

    private val isCallAssertionsDisabled = context.state.isCallAssertionsDisabled
    private val isReceiverAssertionsDisabled = context.state.isReceiverAssertionsDisabled

    override fun lower(irFile: IrFile) {
        irFile.acceptChildrenVoid(this)
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    private inline fun <T : IrElement> T.transformPostfix(fn: T.() -> Unit) {
        acceptChildrenVoid(this@JvmArgumentNullabilityAssertionsLowering)
        fn()
    }

    override fun visitMemberAccess(expression: IrMemberAccessExpression) {
        expression.transformPostfix {

            // Always drop nullability assertions on dispatch receivers, assuming that it will throw NPE.
            //
            // NB there are some members in Kotlin built-in classes which are NOT implemented as platform method calls,
            // and thus break this assertion - e.g., 'Array<T>.iterator()' and similar functions.
            // See KT-30908 for more details.
            dispatchReceiver = dispatchReceiver?.replaceImplicitNotNullWithArgument()

            if (isReceiverAssertionsDisabled || shouldDropNullabilityAssertionOnExtensionReceiver(expression)) {
                extensionReceiver = extensionReceiver?.replaceImplicitNotNullWithArgument()
            }

            if (isCallAssertionsDisabled || isValueArgumentForCallToMethodWithTypeCheckBarrier(expression)) {
                for (i in 0 until expression.valueArgumentsCount) {
                    getValueArgument(i)?.let { irArgument ->
                        putValueArgument(i, irArgument.replaceImplicitNotNullWithArgument())
                    }
                }
            }
        }
    }

    override fun visitContainerExpression(expression: IrContainerExpression) {
        expression.transformPostfix {
            if (isCallAssertionsDisabled) {
                val lastIndex = statements.lastIndex
                if (lastIndex >= 0) {
                    val lastStatement = statements[lastIndex]
                    if (lastStatement is IrExpression) {
                        statements[lastIndex] = lastStatement.replaceImplicitNotNullWithArgument()
                    }
                }
            }
        }
    }

    override fun visitReturn(expression: IrReturn) {
        expression.transformPostfix {
            if (isCallAssertionsDisabled) {
                value = value.replaceImplicitNotNullWithArgument()
            }
        }
    }

    override fun visitSetVariable(expression: IrSetVariable) {
        expression.transformPostfix {
            if (isCallAssertionsDisabled) {
                value = value.replaceImplicitNotNullWithArgument()
            }
        }
    }

    override fun visitGetField(expression: IrGetField) {
        expression.transformPostfix {
            receiver = receiver?.replaceImplicitNotNullWithArgument()
        }
    }

    override fun visitSetField(expression: IrSetField) {
        expression.transformPostfix {
            receiver = receiver?.replaceImplicitNotNullWithArgument()
            if (isCallAssertionsDisabled) {
                value = value.replaceImplicitNotNullWithArgument()
            }
        }
    }

    override fun visitVariable(declaration: IrVariable) {
        declaration.transformPostfix {
            if (isCallAssertionsDisabled) {
                initializer = initializer?.replaceImplicitNotNullWithArgument()
            }
        }
    }

    private fun IrExpressionBody.replaceImplicitNotNullWithArgument() {
        expression = expression.replaceImplicitNotNullWithArgument()
    }

    override fun visitField(declaration: IrField) {
        declaration.transformPostfix {
            if (isCallAssertionsDisabled) {
                initializer?.replaceImplicitNotNullWithArgument()
            }
        }
    }

    override fun visitFunction(declaration: IrFunction) {
        declaration.transformPostfix {
            if (isCallAssertionsDisabled) {
                for (valueParameter in valueParameters) {
                    valueParameter.defaultValue?.replaceImplicitNotNullWithArgument()
                }
            }
        }
    }

    override fun visitWhen(expression: IrWhen) {
        expression.transformPostfix {
            if (isCallAssertionsDisabled) {
                for (irBranch in branches) {
                    irBranch.condition = irBranch.condition.replaceImplicitNotNullWithArgument()
                    irBranch.result = irBranch.result.replaceImplicitNotNullWithArgument()
                }
            }
        }
    }

    override fun visitLoop(loop: IrLoop) {
        loop.transformPostfix {
            if (isCallAssertionsDisabled) {
                condition = condition.replaceImplicitNotNullWithArgument()
            }
        }
    }

    override fun visitThrow(expression: IrThrow) {
        expression.transformPostfix {
            if (isCallAssertionsDisabled) {
                value = value.replaceImplicitNotNullWithArgument()
            }
        }
    }

    override fun visitTry(aTry: IrTry) {
        aTry.transformPostfix {
            if (isCallAssertionsDisabled) {
                tryResult = tryResult.replaceImplicitNotNullWithArgument()
                finallyExpression = finallyExpression?.replaceImplicitNotNullWithArgument()
            }
        }
    }

    override fun visitCatch(aCatch: IrCatch) {
        aCatch.transformPostfix {
            if (isCallAssertionsDisabled) {
                result = result.replaceImplicitNotNullWithArgument()
            }
        }
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall) {
        expression.transformPostfix {
            if (isCallAssertionsDisabled) {
                argument = argument.replaceImplicitNotNullWithArgument()
            }
        }
    }

    override fun visitVararg(expression: IrVararg) {
        expression.transformPostfix {
            if (isCallAssertionsDisabled) {
                elements.forEachIndexed { index, irVarargElement ->
                    when (irVarargElement) {
                        is IrSpreadElement ->
                            irVarargElement.expression = irVarargElement.expression.replaceImplicitNotNullWithArgument()
                        is IrExpression ->
                            putElement(index, irVarargElement.replaceImplicitNotNullWithArgument())
                    }
                }
            }
        }
    }

    private fun shouldDropNullabilityAssertionOnExtensionReceiver(expression: IrMemberAccessExpression): Boolean {
        if (!isWithUnifiedNullChecks) {
            if (expression.origin.isOperatorWithNoNullabilityAssertionsOnExtensionReceiver) return true
        }

        return false
    }

    companion object {
        private val operatorsWithNoNullabilityAssertionsOnExtensionReceiver =
            hashSetOf(
                IrStatementOrigin.PREFIX_INCR, IrStatementOrigin.POSTFIX_INCR,
                IrStatementOrigin.PREFIX_DECR, IrStatementOrigin.POSTFIX_DECR
            )

        internal val IrStatementOrigin?.isOperatorWithNoNullabilityAssertionsOnExtensionReceiver
            get() =
                this is IrStatementOrigin.COMPONENT_N ||
                        this in operatorsWithNoNullabilityAssertionsOnExtensionReceiver

        internal fun IrExpression.replaceImplicitNotNullWithArgument(): IrExpression =
            if (this is IrTypeOperatorCall && this.operator == IrTypeOperator.IMPLICIT_NOTNULL)
                argument
            else
                this

        internal fun isValueArgumentForCallToMethodWithTypeCheckBarrier(expression: IrMemberAccessExpression): Boolean {
            val descriptor = expression.symbol.descriptor
            if (descriptor !is CallableMemberDescriptor) return false

            val specialSignatureInfo = with(BuiltinMethodsWithSpecialGenericSignature) {
                descriptor.getSpecialSignatureInfo()
            }
            return specialSignatureInfo?.isObjectReplacedWithTypeParameter ?: false
        }
    }
}