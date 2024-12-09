/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.ir.copyCorrespondingPropertyFrom
import org.jetbrains.kotlin.backend.jvm.ir.firstSuperMethodFromKotlin
import org.jetbrains.kotlin.backend.jvm.ir.isJvmInterface
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.copyValueAndTypeParametersFrom
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.isClass
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.transformInPlace

/**
 * Generates default compatibility bridges for classes in `-Xjvm-default=all/all-compatibility` modes.
 *
 * "Default compatibility bridge" is a method which is generated in a class in `-Xjvm-default=all/all-compatibility` modes, to keep behavior
 * in diamond hierarchies the same as in the `-Xjvm-default=disable` mode. Example:
 *
 *     interface Base {
 *         fun foo(): String = "Fail"
 *     }
 *     open class Left : Base
 *
 *     interface Right : Base {
 *         override fun foo(): String = "OK"
 *     }
 *     class Bottom : Left(), Right
 *
 * If Base and Left are compiled with `-Xjvm-default=disable`, but Right and Bottom are compiled with `-Xjvm-default=all/all-compatibility`,
 * we generate a _default compatibility bridge_ in Bottom which calls `super<Right>.foo()`. Without this bridge, calls to `Bottom.foo()`
 * would result in "Fail" because there's a DefaultImpls bridge in Left which is inherited in Bottom, and class methods win over default
 * interface methods in JVM during call resolution.
 */
@PhaseDescription(name = "GenerateJvmDefaultCompatibilityBridges")
class GenerateJvmDefaultCompatibilityBridges(private val context: JvmBackendContext) : ClassLoweringPass {
    override fun lower(irClass: IrClass) {
        if (!context.config.jvmDefaultMode.isEnabled) return

        if (irClass.isJvmInterface) return

        irClass.declarations.transformInPlace { declaration ->
            replaceWithDefaultCompatibilityBridgeIfNeeded(declaration, irClass) ?: declaration
        }
    }

    private fun replaceWithDefaultCompatibilityBridgeIfNeeded(declaration: IrDeclaration, irClass: IrClass): IrDeclaration? {
        if (declaration !is IrSimpleFunction || !declaration.isFakeOverride) return null
        val implementation = declaration.findInterfaceImplementation(context.config.jvmDefaultMode, allowJvmDefault = true) ?: return null
        if (!needsJvmDefaultCompatibilityBridge(declaration)) return null
        return generateBridge(declaration, implementation, irClass)
    }

    // Generating the default compatibility bridge is only necessary if there's a risk that some other method will be called at runtime
    // by JVM when resolving the call to method from this class. This is only possible when this class has a superclass, where this method
    // is present as a bridge to DefaultImpls of some other interface.
    private fun needsJvmDefaultCompatibilityBridge(declaration: IrSimpleFunction): Boolean {
        var current: IrSimpleFunction? = declaration.overriddenSymbols.singleOrNull { it.owner.parentAsClass.isClass }?.owner
        while (current != null) {
            if (current.modality == Modality.ABSTRACT) return false

            if (current.findInterfaceImplementation(context.config.jvmDefaultMode) != null) return true

            current = current.overriddenSymbols.singleOrNull { it.owner.parentAsClass.isClass }?.owner
        }
        return false
    }

    private fun generateBridge(declaration: IrSimpleFunction, implementation: IrSimpleFunction, irClass: IrClass): IrSimpleFunction {
        return context.irFactory.buildFun {
            origin = JvmLoweredDeclarationOrigin.SUPER_INTERFACE_METHOD_BRIDGE
            name = declaration.name
            visibility = declaration.visibility
            modality = declaration.modality
            returnType = declaration.returnType
            isInline = declaration.isInline
            isExternal = false
            isTailrec = false
            isSuspend = declaration.isSuspend
            isOperator = declaration.isOperator
            isInfix = declaration.isInfix
            isExpect = false
            isFakeOverride = false
        }.apply {
            parent = irClass
            overriddenSymbols = declaration.overriddenSymbols
            copyValueAndTypeParametersFrom(declaration)
            dispatchReceiverParameter?.type = irClass.defaultType
            annotations = declaration.annotations
            copyCorrespondingPropertyFrom(declaration)

            context.createIrBuilder(symbol, irClass.startOffset, irClass.endOffset).apply {
                body = irExprBody(irBlock {
                    val superMethod = firstSuperMethodFromKotlin(declaration, implementation)
                    +irCall(superMethod, returnType).apply {
                        superQualifierSymbol = superMethod.owner.parentAsClass.symbol

                        dispatchReceiver = irGet(dispatchReceiverParameter!!)
                        extensionReceiverParameter?.let { extensionReceiver = irGet(it) }
                        for ((index, parameter) in typeParameters.withIndex()) {
                            putTypeArgument(index, parameter.defaultType)
                        }
                        for ((index, parameter) in valueParameters.withIndex()) {
                            putValueArgument(index, irGet(parameter))
                        }
                    }
                })
            }
        }
    }
}
