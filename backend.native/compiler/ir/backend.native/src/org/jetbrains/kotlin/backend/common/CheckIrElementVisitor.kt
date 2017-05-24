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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.makeNullable

typealias ReportError = (element: IrElement, message: String) -> Unit

class CheckIrElementVisitor(val builtIns: KotlinBuiltIns, val reportError: ReportError, val ensureAllNodesAreDifferent: Boolean) : IrElementVisitorVoid {

    val set = mutableSetOf<IrElement>()

    override fun visitElement(element: IrElement) {
        if (ensureAllNodesAreDifferent) {
            if (set.contains(element))
                reportError(element, "Duplicate IR node")
            set.add(element)
        }
        // Nothing to do.
    }

    private fun IrExpression.ensureTypeIs(expectedType: KotlinType) {
        if (expectedType != type) {
            reportError(this, "unexpected expression.type: expected $expectedType, got ${type}")
        }
    }

    private fun IrSymbol.ensureBound(expression: IrExpression) {
        if (!this.isBound) {
            reportError(expression, "Unbound symbol ${this}")
        }
    }

    override fun <T> visitConst(expression: IrConst<T>) {
        super.visitConst(expression)

        val naturalType = when (expression.kind) {
            IrConstKind.Null -> builtIns.nullableNothingType
            IrConstKind.Boolean -> builtIns.booleanType
            IrConstKind.Char -> builtIns.charType
            IrConstKind.Byte -> builtIns.byteType
            IrConstKind.Short -> builtIns.shortType
            IrConstKind.Int -> builtIns.intType
            IrConstKind.Long -> builtIns.longType
            IrConstKind.String -> builtIns.stringType
            IrConstKind.Float -> builtIns.floatType
            IrConstKind.Double -> builtIns.doubleType
        }

        expression.ensureTypeIs(naturalType)
    }

    override fun visitStringConcatenation(expression: IrStringConcatenation) {
        super.visitStringConcatenation(expression)

        expression.ensureTypeIs(builtIns.stringType)
    }

    override fun visitGetObjectValue(expression: IrGetObjectValue) {
        super.visitGetObjectValue(expression)

        expression.ensureTypeIs(expression.descriptor.defaultType)
    }

    // TODO: visitGetEnumValue

    override fun visitGetValue(expression: IrGetValue) {
        super.visitGetValue(expression)

        expression.ensureTypeIs(expression.descriptor.type)
    }

    override fun visitSetVariable(expression: IrSetVariable) {
        super.visitSetVariable(expression)

        expression.ensureTypeIs(builtIns.unitType)
    }

    override fun visitGetField(expression: IrGetField) {
        super.visitGetField(expression)

        expression.ensureTypeIs(expression.descriptor.type)
    }

    override fun visitSetField(expression: IrSetField) {
        super.visitSetField(expression)

        expression.ensureTypeIs(builtIns.unitType)
    }

    override fun visitCall(expression: IrCall) {
        super.visitCall(expression)

        val returnType = expression.descriptor.returnType
        if (returnType == null) {
            reportError(expression, "${expression.descriptor} return type is null")
        } else {
            expression.ensureTypeIs(returnType)
        }

        expression.superQualifierSymbol?.ensureBound(expression)
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall) {
        super.visitDelegatingConstructorCall(expression)

        expression.ensureTypeIs(builtIns.unitType)
    }

    override fun visitEnumConstructorCall(expression: IrEnumConstructorCall) {
        super.visitEnumConstructorCall(expression)

        expression.ensureTypeIs(builtIns.unitType)
    }

    override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall) {
        super.visitInstanceInitializerCall(expression)

        expression.ensureTypeIs(builtIns.unitType)
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
            IrTypeOperator.IMPLICIT_INTEGER_COERCION -> typeOperand

            IrTypeOperator.SAFE_CAST -> typeOperand.makeNullable()

            IrTypeOperator.INSTANCEOF, IrTypeOperator.NOT_INSTANCEOF -> builtIns.booleanType
        }

        if (operator == IrTypeOperator.IMPLICIT_COERCION_TO_UNIT && typeOperand != builtIns.unitType) {
            reportError(expression, "typeOperand is $typeOperand")
        }

        // TODO: check IMPLICIT_NOTNULL's argument type.

        expression.ensureTypeIs(naturalType)
    }

    override fun visitLoop(loop: IrLoop) {
        super.visitLoop(loop)

        loop.ensureTypeIs(builtIns.unitType)
    }

    override fun visitBreakContinue(jump: IrBreakContinue) {
        super.visitBreakContinue(jump)

        jump.ensureTypeIs(builtIns.nothingType)
    }

    override fun visitReturn(expression: IrReturn) {
        super.visitReturn(expression)

        expression.ensureTypeIs(builtIns.nothingType)
        expression.returnTargetSymbol.ensureBound(expression)
    }

    override fun visitThrow(expression: IrThrow) {
        super.visitThrow(expression)

        expression.ensureTypeIs(builtIns.nothingType)
    }

    override fun visitClass(declaration: IrClass) {
        super.visitClass(declaration)

        if (declaration.descriptor.kind != ClassKind.ANNOTATION_CLASS) {
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

    override fun visitDeclarationReference(expression: IrDeclarationReference) {
        super.visitDeclarationReference(expression)

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

}