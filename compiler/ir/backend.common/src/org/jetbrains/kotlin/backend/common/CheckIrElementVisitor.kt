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

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.isAnnotationClass
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyExternal

typealias ReportError = (element: IrElement, message: String) -> Unit

class CheckIrElementVisitor(
    val irBuiltIns: IrBuiltIns,
    val reportError: ReportError,
    val config: IrValidatorConfig
) : IrElementVisitorVoid {

    val set = mutableSetOf<IrElement>()
    val checkedTypes = mutableSetOf<IrType>()

    override fun visitElement(element: IrElement) {
        if (config.ensureAllNodesAreDifferent) {
            if (set.contains(element))
                reportError(element, "Duplicate IR node")
            set.add(element)
        }
        // Nothing to do.
    }

    private fun IrExpression.ensureTypeIs(expectedType: IrType) {
        if (!config.checkTypes)
            return

        // TODO: compare IR types instead.
        if (expectedType.isEqualTo(type)) {
            reportError(this, "unexpected expression.type: expected $expectedType, got ${type.render()}")
        }
    }

    private fun IrSymbol.ensureBound(expression: IrExpression) {
        if (!this.isBound && expression.type !is IrDynamicType) {
            reportError(expression, "Unbound symbol ${this}")
        }
    }

    override fun <T> visitConst(expression: IrConst<T>) {
        super.visitConst(expression)

        val naturalType = when (expression.kind) {
            IrConstKind.Null -> irBuiltIns.nothingNType
            IrConstKind.Boolean -> irBuiltIns.booleanType
            IrConstKind.Char -> irBuiltIns.charType
            IrConstKind.Byte -> irBuiltIns.byteType
            IrConstKind.Short -> irBuiltIns.shortType
            IrConstKind.Int -> irBuiltIns.intType
            IrConstKind.Long -> irBuiltIns.longType
            IrConstKind.String -> irBuiltIns.stringType
            IrConstKind.Float -> irBuiltIns.floatType
            IrConstKind.Double -> irBuiltIns.doubleType
        }

        expression.ensureTypeIs(naturalType)
    }

    override fun visitStringConcatenation(expression: IrStringConcatenation) {
        super.visitStringConcatenation(expression)

        expression.ensureTypeIs(irBuiltIns.stringType)
    }

    override fun visitGetObjectValue(expression: IrGetObjectValue) {
        super.visitGetObjectValue(expression)

        expression.ensureTypeIs(expression.symbol.createType(false, emptyList()))
    }

    // TODO: visitGetEnumValue

    override fun visitGetValue(expression: IrGetValue) {
        super.visitGetValue(expression)

        expression.ensureTypeIs(expression.symbol.owner.type)
    }

    override fun visitSetVariable(expression: IrSetVariable) {
        super.visitSetVariable(expression)

        expression.ensureTypeIs(irBuiltIns.unitType)
    }

    override fun visitGetField(expression: IrGetField) {
        super.visitGetField(expression)

        expression.ensureTypeIs(expression.symbol.owner.type)
    }

    override fun visitSetField(expression: IrSetField) {
        super.visitSetField(expression)

        expression.ensureTypeIs(irBuiltIns.unitType)
    }

    override fun visitCall(expression: IrCall) {
        super.visitCall(expression)

        val function = expression.symbol.owner

        if (function.dispatchReceiverParameter?.type is IrDynamicType) {
            reportError(expression, "Dispatch receivers with 'dynamic' type are not allowed")
        }

        val returnType = expression.symbol.owner.returnType
        expression.ensureTypeIs(returnType)

        expression.superQualifierSymbol?.ensureBound(expression)
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall) {
        super.visitDelegatingConstructorCall(expression)

        expression.ensureTypeIs(irBuiltIns.unitType)
    }

    override fun visitEnumConstructorCall(expression: IrEnumConstructorCall) {
        super.visitEnumConstructorCall(expression)

        expression.ensureTypeIs(irBuiltIns.unitType)
    }

    override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall) {
        super.visitInstanceInitializerCall(expression)

        expression.ensureTypeIs(irBuiltIns.unitType)
        expression.classSymbol.ensureBound(expression)
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall) {
        super.visitTypeOperator(expression)

        val operator = expression.operator
        val typeOperand = expression.typeOperand

        val naturalType = when (operator) {
            IrTypeOperator.CAST,
            IrTypeOperator.IMPLICIT_CAST,
            IrTypeOperator.IMPLICIT_NOTNULL,
            IrTypeOperator.IMPLICIT_COERCION_TO_UNIT,
            IrTypeOperator.IMPLICIT_INTEGER_COERCION,
            IrTypeOperator.SAM_CONVERSION -> typeOperand

            IrTypeOperator.SAFE_CAST -> typeOperand.makeNullable()

            IrTypeOperator.INSTANCEOF, IrTypeOperator.NOT_INSTANCEOF -> irBuiltIns.booleanType
        }

        if (operator == IrTypeOperator.IMPLICIT_COERCION_TO_UNIT && !typeOperand.isUnit()) {
            reportError(expression, "typeOperand is ${typeOperand.render()}")
        }

        // TODO: check IMPLICIT_NOTNULL's argument type.

        expression.ensureTypeIs(naturalType)
    }

    override fun visitLoop(loop: IrLoop) {
        super.visitLoop(loop)

        loop.ensureTypeIs(irBuiltIns.unitType)
    }

    override fun visitBreakContinue(jump: IrBreakContinue) {
        super.visitBreakContinue(jump)

        jump.ensureTypeIs(irBuiltIns.nothingType)
    }

    override fun visitReturn(expression: IrReturn) {
        super.visitReturn(expression)

        expression.ensureTypeIs(irBuiltIns.nothingType)
        expression.returnTargetSymbol.ensureBound(expression)
    }

    override fun visitThrow(expression: IrThrow) {
        super.visitThrow(expression)

        expression.ensureTypeIs(irBuiltIns.nothingType)
    }

    override fun visitClass(declaration: IrClass) {
        super.visitClass(declaration)

        if (config.checkDescriptors && !declaration.isAnnotationClass) {
            // Check that all functions and properties from memberScope are present in IR
            // (including FAKE_OVERRIDE ones).

            val allDescriptors = declaration.descriptor.unsubstitutedMemberScope
                    .getContributedDescriptors().filterIsInstance<CallableMemberDescriptor>()

            val presentDescriptors = declaration.declarations.map { it.descriptor }

            val missingDescriptors = allDescriptors - presentDescriptors

            if (missingDescriptors.isNotEmpty()) {
                reportError(declaration, "Missing declarations for descriptors:\n" +
                        missingDescriptors.joinToString("\n"))
            }
        }
    }

    override fun visitFunction(declaration: IrFunction) {
        super.visitFunction(declaration)

        if (declaration.dispatchReceiverParameter?.type is IrDynamicType) {
            reportError(declaration, "Dispatch receivers with 'dynamic' type are not allowed")
        }

        for ((i, p) in declaration.valueParameters.withIndex()) {
            if (p.index != i) {
                reportError(declaration, "Inconsistent index of value parameter ${p.index} != $i")
            }
        }

        for ((i, p) in declaration.typeParameters.withIndex()) {
            if (p.index != i) {
                reportError(declaration, "Inconsistent index of type parameter ${p.index} != $i")
            }
        }
    }

    override fun visitDeclarationReference(expression: IrDeclarationReference) {
        super.visitDeclarationReference(expression)

        // TODO: Fix unbound external declarations
        if (expression.descriptor.isEffectivelyExternal())
            return

        // TODO: Fix unbound dynamic filed declarations
        if (expression is IrFieldAccessExpression) {
            val receiverType = expression.receiver?.type
            if (receiverType is IrDynamicType)
                return
        }

        expression.symbol.ensureBound(expression)
    }

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression) {
        super.visitFunctionAccess(expression)

        expression.symbol.ensureBound(expression)
    }

    override fun visitFunctionReference(expression: IrFunctionReference) {
        super.visitFunctionReference(expression)

        expression.symbol.ensureBound(expression)
    }

    override fun visitPropertyReference(expression: IrPropertyReference) {
        super.visitPropertyReference(expression)

        expression.field?.ensureBound(expression)
        expression.getter?.ensureBound(expression)
        expression.setter?.ensureBound(expression)
    }

    override fun visitLocalDelegatedPropertyReference(expression: IrLocalDelegatedPropertyReference) {
        super.visitLocalDelegatedPropertyReference(expression)

        expression.delegate.ensureBound(expression)
        expression.getter.ensureBound(expression)
        expression.setter?.ensureBound(expression)
    }

    override fun visitExpression(expression: IrExpression) {
        checkType(expression.type, expression)
        super.visitExpression(expression)
    }

    private fun checkType(type: IrType, element: IrElement) {
        if (type in checkedTypes)
            return

        when (type) {
            is IrSimpleType -> {
                if (!type.classifier.isBound) {
                    reportError(element, "Type: ${type.render()} has unbound classifier")
                }
            }
        }

        checkedTypes.add(type)
    }
}