/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ScopeWithIr
import org.jetbrains.kotlin.backend.common.lower.SingleAbstractMethodLowering
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.erasedUpperBound
import org.jetbrains.kotlin.backend.jvm.ir.rawType
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.IrFunctionBuilder
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.load.java.JavaDescriptorVisibilities
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

internal val singleAbstractMethodPhase = makeIrFilePhase(
    ::JvmSingleAbstractMethodLowering,
    name = "SingleAbstractMethod",
    description = "Replace SAM conversions with instances of interface-implementing classes"
)

private class JvmSingleAbstractMethodLowering(context: JvmBackendContext) : SingleAbstractMethodLowering(context) {
    // The JVM BE produces inline SAM wrappers both in the scope of an inline function as well as in the scope
    // of an inline lambda. Compare with `SamWrapperClasses.getSamWrapperClass`.
    override val inInlineFunctionScope: Boolean
        get() = allScopes.any { scope ->
            scope.irElement.safeAs<IrFunction>()?.let {
                it.isInline || it.origin == IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
            } ?: false
        }

    override fun getWrapperVisibility(expression: IrTypeOperatorCall, scopes: List<ScopeWithIr>) =
        if (inInlineFunctionScope) DescriptorVisibilities.PUBLIC else JavaDescriptorVisibilities.PACKAGE_VISIBILITY

    override fun getSuperTypeForWrapper(typeOperand: IrType): IrType =
        typeOperand.erasedUpperBound.rawType(context as JvmBackendContext)

    override fun getWrappedFunctionType(klass: IrClass): IrType =
        klass.rawType(context as JvmBackendContext)

    // The constructor of a SAM wrapper is non-synthetic and should not have line numbers.
    // Otherwise the debugger will try to step into it.
    override fun IrFunctionBuilder.setConstructorSourceRange(createFor: IrElement) {
        startOffset = UNDEFINED_OFFSET
        endOffset = UNDEFINED_OFFSET
    }

    private val IrType.isKotlinFunInterface: Boolean
        get() = getClass()?.origin != IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB

    override val IrType.needEqualsHashCodeMethods get() = isKotlinFunInterface
}
