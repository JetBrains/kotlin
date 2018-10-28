/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.makePhase
import org.jetbrains.kotlin.backend.common.descriptors.WrappedSimpleFunctionDescriptor
import org.jetbrains.kotlin.backend.common.ir.copyParameterDeclarationsFrom
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.JvmLoweredStatementOrigin
import org.jetbrains.kotlin.backend.jvm.codegen.isJvmInterface
import org.jetbrains.kotlin.codegen.OwnerKind
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.deserialization.PLATFORM_DEPENDENT_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.resolveFakeOverride
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor
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
        for ((interfaceFun, value) in actualClass.getNonPrivateInterfaceMethods()) {
            //skip java 8 default methods
            if (!interfaceFun.isDefinitelyNotDefaultImplsMethod() && !interfaceFun.isMethodOfAny()) {
                generateDelegationToDefaultImpl(irClass, interfaceFun, value, isDefaultImplsGeneration)
            }
        }

//        CodegenUtil.getNonPrivateTraitMethods(actualClass.descriptor, !isDefaultImplsGeneration)) {
//            //skip java 8 default methods
//            if (!interfaceFun.isDefinitelyNotDefaultImplsMethod() && !FunctionCodegen.isMethodOfAny(interfaceFun)) {
//                generateDelegationToDefaultImpl(
//                    irClass, context.ir.symbols.externalSymbolTable.referenceSimpleFunction(
//                        interfaceFun.original
//                    ).owner, value, isDefaultImplsGeneration
//                )
//            }
//        }
    }

    private fun generateDelegationToDefaultImpl(
        irClass: IrClass,
        interfaceFun: IrFunction,
        inheritedFun: IrSimpleFunction,
        isDefaultImplsGeneration: Boolean
    ) {
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
                    isInline = inheritedFun.isInline,
                    isExternal = false,
                    isTailrec = false,
                    isSuspend = inheritedFun.isSuspend
                ).apply {
                    descriptor.bind(this)
                    parent = inheritedFun.parent
                    returnType = inheritedFun.returnType
                    overriddenSymbols.addAll(inheritedFun.overriddenSymbols)
                    copyParameterDeclarationsFrom(inheritedFun)
                }
            } else context.declarationFactory.getDefaultImplsFunction(inheritedFun)
        val irBody = IrBlockBodyImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET)
        irFunction.body = irBody
        irClass.declarations.add(irFunction)

        val irCallImpl =
            IrCallImpl(
                UNDEFINED_OFFSET,
                UNDEFINED_OFFSET,
                defaultImplFun.returnType,
                defaultImplFun.symbol,
                defaultImplFun.descriptor,
                origin = JvmLoweredStatementOrigin.DEFAULT_IMPLS_DELEGATION
            )
        irBody.statements.add(
            IrReturnImpl(
                UNDEFINED_OFFSET,
                UNDEFINED_OFFSET,
                irFunction.returnType,
                irFunction.symbol,
                irCallImpl
            )
        )

        var offset = 0
        irFunction.dispatchReceiverParameter?.let {
            irCallImpl.putValueArgument(
                offset,
                IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, it.symbol)
            )
            offset++
        }

        irFunction.extensionReceiverParameter?.let {
            irCallImpl.putValueArgument(
                offset,
                IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, it.symbol)
            )
            offset++
        }

        irFunction.valueParameters.mapIndexed { i, parameter ->
            irCallImpl.putValueArgument(i + offset, IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, parameter.symbol, null))
        }
    }

    private fun IrSimpleFunction.isMethodOfAny() =
        ((valueParameters.size == 0 && name.asString() in setOf("hashCode", "toString")) ||
                (valueParameters.size == 1 && name.asString() == "equals" && valueParameters[0].type == context.irBuiltIns.anyType))

    private fun IrSimpleFunction.isDefinitelyNotDefaultImplsMethod() =
        resolveFakeOverride()?.let {
            origin == IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB && descriptor is JavaCallableMemberDescriptor
        } == true ||
                hasAnnotation(PLATFORM_DEPENDENT_ANNOTATION_FQ_NAME)

    private fun IrClass.getNonPrivateInterfaceMethods(): List<Pair<IrSimpleFunction, IrSimpleFunction>> {
        return declarations.filterIsInstance<IrSimpleFunction>().mapNotNull { function ->
            val resolved = function.resolveFakeOverride()
            resolved?.takeIf {
                resolved !== function && // TODO: take a better look
                        (resolved.parent as? IrClass)?.isInterface == true &&
                        !Visibilities.isPrivate(resolved.visibility) &&
                        resolved.visibility != Visibilities.INVISIBLE_FAKE
            }?.let { Pair(resolved, function) }
        }
    }
}
