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
import org.jetbrains.kotlin.backend.jvm.ir.isInPublicInlineScope
import org.jetbrains.kotlin.backend.jvm.ir.rawType
import org.jetbrains.kotlin.backend.jvm.ir.suspendFunctionOriginal
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.IrFunctionBuilder
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.load.java.JavaDescriptorVisibilities
import org.jetbrains.kotlin.utils.filterIsInstanceAnd

internal val singleAbstractMethodPhase = makeIrFilePhase(
    ::JvmSingleAbstractMethodLowering,
    name = "SingleAbstractMethod",
    description = "Replace SAM conversions with instances of interface-implementing classes",
    // FunctionReferenceLowering produces optimized SAM wrappers.
    prerequisite = setOf(functionReferencePhase),
)

private class JvmSingleAbstractMethodLowering(context: JvmBackendContext) : SingleAbstractMethodLowering(context) {

    private val isJavaSamConversionWithEqualsHashCode =
        context.config.languageVersionSettings.supportsFeature(LanguageFeature.JavaSamConversionEqualsHashCode)

    override val inInlineFunctionScope: Boolean
        get() = allScopes.any { (it.irElement as? IrDeclaration)?.isInPublicInlineScope == true }

    override fun getWrapperVisibility(expression: IrTypeOperatorCall, scopes: List<ScopeWithIr>) =
        if (inInlineFunctionScope) DescriptorVisibilities.PUBLIC else JavaDescriptorVisibilities.PACKAGE_VISIBILITY

    override fun getSuperTypeForWrapper(typeOperand: IrType): IrType =
        typeOperand.erasedUpperBound.rawType(context as JvmBackendContext)

    override fun getWrappedFunctionType(klass: IrClass): IrType =
        klass.rawType(context as JvmBackendContext)

    override fun getSuspendFunctionWithoutContinuation(function: IrSimpleFunction): IrSimpleFunction =
        function.suspendFunctionOriginal()

    // The constructor of a SAM wrapper is non-synthetic and should not have line numbers.
    // Otherwise the debugger will try to step into it.
    override fun IrFunctionBuilder.setConstructorSourceRange(createFor: IrElement) {
        startOffset = UNDEFINED_OFFSET
        endOffset = UNDEFINED_OFFSET
    }

    private val IrType.isKotlinFunInterface: Boolean
        get() = getClass()?.origin != IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB

    override val IrType.needEqualsHashCodeMethods
        get() = isKotlinFunInterface || isJavaSamConversionWithEqualsHashCode

    override fun postprocessCreatedObjectProxy(klass: IrClass) {
        val fakeOverrideProperties = klass.declarations.filterIsInstanceAnd(IrProperty::isFakeOverride)
        klass.declarations.removeAll(fakeOverrideProperties)
        for (property in fakeOverrideProperties) {
            property.getter?.let(klass.declarations::add)
            property.setter?.let(klass.declarations::add)
        }
    }
}
