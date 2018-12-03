/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.descriptors.WrappedSimpleFunctionDescriptor
import org.jetbrains.kotlin.backend.common.ir.copyParameterDeclarationsFrom
import org.jetbrains.kotlin.backend.common.ir.isMethodOfAny
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.codegen.isJvmInterface
import org.jetbrains.kotlin.codegen.OwnerKind
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.deserialization.PLATFORM_DEPENDENT_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.name.Name
import java.util.function.UnaryOperator

internal val interfaceDelegationPhase = makeIrFilePhase(
    ::InterfaceDelegationLowering,
    name = "InterfaceDelegation",
    description = "Delegate calls to interface members with default implementations to DefaultImpls"
)

private class InterfaceDelegationLowering(val context: JvmBackendContext) : IrElementVisitorVoid, FileLoweringPass {

    val state: GenerationState = context.state

    val replacementMap = mutableMapOf<IrSimpleFunctionSymbol, IrSimpleFunctionSymbol>()

    override fun lower(irFile: IrFile) {
        irFile.acceptChildrenVoid(this)
        // TODO: Replacer should be run on whole module, not on a single file.
        irFile.acceptVoid(OverriddenSymbolsReplacer(replacementMap))
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitClass(declaration: IrClass) {
        super.visitClass(declaration)
        if (declaration.isJvmInterface) return

        generateInterfaceMethods(declaration)
    }

    private fun generateInterfaceMethods(irClass: IrClass) {
        val (actualClass, isDefaultImplsGeneration) = if (irClass.origin == JvmLoweredDeclarationOrigin.DEFAULT_IMPLS) {
            Pair(irClass.parent as IrClass, true)
        } else {
            Pair(irClass, false)
        }

        val toRemove = mutableListOf<IrSimpleFunction>()
        for (function in actualClass.functions.toList()) { // Copy the list, because we are adding new declarations from the loop
            if (function.origin !== IrDeclarationOrigin.FAKE_OVERRIDE) continue

            // In classes, only generate interface delegation for functions immediately inherited from am interface.
            // (Otherwise, delegation will be present in the parent class)
            if (!isDefaultImplsGeneration &&
                function.overriddenSymbols.any {
                    !it.owner.parentAsClass.isInterface &&
                            it.owner.modality != Modality.ABSTRACT
                }
            ) {
                continue
            }

            val implementation = function.resolveFakeOverride() ?: continue
            if (!implementation.hasInterfaceParent() ||
                Visibilities.isPrivate(implementation.visibility) ||
                implementation.isDefinitelyNotDefaultImplsMethod() || implementation.isMethodOfAny()
            ) {
                continue
            }

            val delegation = generateDelegationToDefaultImpl(irClass, implementation, function, isDefaultImplsGeneration)
            if (!isDefaultImplsGeneration) {
                toRemove.add(function)
                replacementMap[function.symbol] = delegation.symbol
            }
        }
        irClass.declarations.removeAll(toRemove)
    }

    private fun generateDelegationToDefaultImpl(
        irClass: IrClass,
        interfaceFun: IrSimpleFunction,
        inheritedFun: IrSimpleFunction,
        isDefaultImplsGeneration: Boolean
    ): IrSimpleFunction {
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

        irClass.declarations.add(irFunction)

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

    private class OverriddenSymbolsReplacer(val replacementMap: Map<IrSimpleFunctionSymbol, IrSimpleFunctionSymbol>) :
        IrElementVisitorVoid {

        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitSimpleFunction(declaration: IrSimpleFunction) {
            declaration.overriddenSymbols.replaceAll(UnaryOperator { symbol -> replacementMap[symbol] ?: symbol })
            super.visitSimpleFunction(declaration)
        }
    }

    private fun IrSimpleFunction.hasInterfaceParent() =
        (parent as? IrClass)?.isInterface == true

    private fun IrSimpleFunction.isDefinitelyNotDefaultImplsMethod() =
        resolveFakeOverride()?.let { origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB } == true ||
                origin == IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER ||
                hasAnnotation(PLATFORM_DEPENDENT_ANNOTATION_FQ_NAME)
}
