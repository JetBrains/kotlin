/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.MemoizedValueClassAbstractReplacements
import org.jetbrains.kotlin.backend.jvm.isMultiFieldValueClassFieldGetter
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*

private class JvmMultiFieldValueClassLowering(context: JvmBackendContext) : JvmValueClassAbstractLowering(context) {
    override val replacements: MemoizedValueClassAbstractReplacements
        get() = context.multiFieldValueClassReplacements

    override fun IrClass.isSpecificLoweringLogicApplicable(): Boolean = isMultiFieldValueClass

    override fun IrFunction.isSpecificFieldGetter(): Boolean = isMultiFieldValueClassFieldGetter

    override fun transformSimpleFunctionFlat(function: IrSimpleFunction, replacement: IrSimpleFunction): List<IrDeclaration> {
        TODO()
    }

    override fun buildPrimaryValueClassConstructor(valueClass: IrClass, irConstructor: IrConstructor) {
        TODO("Not yet implemented")
    }

    override fun buildBoxFunction(valueClass: IrClass) {
        TODO("Not yet implemented")
    }

    override fun buildUnboxFunctions(valueClass: IrClass) {
        TODO("Not yet implemented")
    }

    override fun buildSpecializedEqualsMethod(valueClass: IrClass) {
        TODO("Not yet implemented")
    }

    @Suppress("UNUSED_PARAMETER")
    override fun transformConstructorFlat(constructor: IrConstructor, replacement: IrSimpleFunction): List<IrDeclaration> {
        TODO()
    }

    override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
        // todo implement
        return super.visitFunctionReference(expression)
    }

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
        // todo implement
        return super.visitFunctionAccess(expression)
    }

    override fun visitCall(expression: IrCall): IrExpression {
        // todo implement
        return super.visitCall(expression)
    }

    override fun visitGetField(expression: IrGetField): IrExpression {
        // todo implement
        return super.visitGetField(expression)
    }

    override fun visitGetValue(expression: IrGetValue): IrExpression {
        // todo implement
        return super.visitGetValue(expression)
    }

    override fun visitSetValue(expression: IrSetValue): IrExpression {
        // todo implement
        return super.visitSetValue(expression)
    }
}