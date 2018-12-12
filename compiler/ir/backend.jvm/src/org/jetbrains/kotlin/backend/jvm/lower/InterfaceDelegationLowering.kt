/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.descriptors.WrappedSimpleFunctionDescriptor
import org.jetbrains.kotlin.backend.common.ir.copyParameterDeclarationsFrom
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.codegen.isJvmInterface
import org.jetbrains.kotlin.codegen.OwnerKind
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.deserialization.PLATFORM_DEPENDENT_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.resolveFakeOverride
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

class InterfaceDelegationLowering(val context: JvmBackendContext) : IrElementTransformerVoid(), ClassLoweringPass {

    val state: GenerationState = context.state

    override fun lower(irClass: IrClass) {
        if (irClass.isJvmInterface) return

        irClass.transformChildrenVoid(this)
        generateInterfaceMethods(irClass)
    }


    private fun generateInterfaceMethods(irClass: IrClass) {
        val (actualClass, isDefaultImplsGeneration) = if (irClass.origin == JvmLoweredDeclarationOrigin.DEFAULT_IMPLS) {
            Pair(irClass.parent as IrClass, true)
        } else {
            Pair(irClass, false)
        }

        val newDeclarations = mutableListOf<IrFunction>()
        for (function in actualClass.declarations) {
            if (function !is IrSimpleFunction) continue
            if (function.origin !== IrDeclarationOrigin.FAKE_OVERRIDE) continue

            val implementation = function.resolveFakeOverride() ?: continue
            if (!implementation.hasInterfaceParent() ||
                Visibilities.isPrivate(implementation.visibility) ||
                implementation.visibility === Visibilities.INVISIBLE_FAKE ||
                implementation.isDefinitelyNotDefaultImplsMethod() || implementation.isMethodOfAny()
            ) {
                continue
            }

            newDeclarations.add(generateDelegationToDefaultImpl(implementation, function, isDefaultImplsGeneration))
        }

        irClass.declarations.addAll(newDeclarations)
    }

    private fun generateDelegationToDefaultImpl(
        interfaceFun: IrSimpleFunction,
        inheritedFun: IrSimpleFunction,
        isDefaultImplsGeneration: Boolean
    ): IrFunction {
        val defaultImplFun = context.declarationFactory.getDefaultImplsFunction(interfaceFun)

        val irFunction =
            if (!isDefaultImplsGeneration) {
                val descriptor = WrappedSimpleFunctionDescriptor(inheritedFun.descriptor.annotations, inheritedFun.descriptor.source)
                /*
                    By using WrappedDescriptor, we lose information whether the function is an accessor.
                    `KotlinTypeMapper` needs that info to generate JVM name.
                    TODO: streamline name generation.
                 */
                val name = Name.identifier(context.state.typeMapper.mapFunctionName(inheritedFun.descriptor, OwnerKind.IMPLEMENTATION))
                IrFunctionImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    IrDeclarationOrigin.DEFINED,
                    IrSimpleFunctionSymbolImpl(descriptor),
                    name,
                    Visibilities.PUBLIC,
                    inheritedFun.modality,
                    inheritedFun.returnType,
                    isInline = inheritedFun.isInline,
                    isExternal = false,
                    isTailrec = false,
                    isSuspend = inheritedFun.isSuspend
                ).apply {
                    descriptor.bind(this)
                    parent = inheritedFun.parent
                    overriddenSymbols.addAll(inheritedFun.overriddenSymbols)
                    copyParameterDeclarationsFrom(inheritedFun)
                }
            } else context.declarationFactory.getDefaultImplsFunction(inheritedFun)

        context.createIrBuilder(irFunction.symbol, UNDEFINED_OFFSET, UNDEFINED_OFFSET).apply {
            irFunction.body = irBlockBody {
                +irReturn(
                    irCall(defaultImplFun.symbol, irFunction.returnType).apply {
                        var offset = 0
                        irFunction.dispatchReceiverParameter?.let { putValueArgument(offset++, irGet(it)) }
                        irFunction.extensionReceiverParameter?.let { putValueArgument(offset++, irGet(it)) }
                        irFunction.valueParameters.mapIndexed { i, parameter -> putValueArgument(i + offset, irGet(parameter)) }
                    }
                )
            }
        }

        return irFunction
    }

    private fun IrSimpleFunction.isMethodOfAny() =
        ((valueParameters.size == 0 && name.asString() in setOf("hashCode", "toString")) ||
                (valueParameters.size == 1 && name.asString() == "equals" && valueParameters[0].type == context.irBuiltIns.anyType))

    private fun IrSimpleFunction.hasInterfaceParent() =
        (parent as? IrClass)?.isInterface == true

    private fun IrSimpleFunction.isDefinitelyNotDefaultImplsMethod() =
        resolveFakeOverride()?.let { origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB } == true ||
                origin == IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER ||
                hasAnnotation(PLATFORM_DEPENDENT_ANNOTATION_FQ_NAME)
}
