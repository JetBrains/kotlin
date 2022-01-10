/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.descriptors.synthesizedString
import org.jetbrains.kotlin.backend.common.ir.*
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrErrorExpressionImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.isExternalOrInheritedFromExternal
import org.jetbrains.kotlin.name.Name
import java.util.concurrent.ConcurrentHashMap

class DefaultArgumentsHelper(
    val skipInlineMethods: Boolean = true,
    val skipExternalMethods: Boolean = false,
    val forceSetOverrideSymbols: Boolean = true
) {
    private val baseFunctionCache = ConcurrentHashMap<IrFunction, IrFunction>()

    fun findBaseFunctionWithDefaultArguments(irFunction: IrFunction): IrFunction? {
        val visited = HashSet<IrFunction>()

        fun dfsImpl(irFunction: IrFunction): IrFunction? {
            baseFunctionCache[irFunction]?.let { return it }

            visited += irFunction
            if (irFunction.isInline && skipInlineMethods) return null
            if (skipExternalMethods && irFunction.isExternalOrInheritedFromExternal()) return null

            if (irFunction is IrSimpleFunction) {
                for (overriddenSymbol in irFunction.overriddenSymbols) {
                    val overridden = overriddenSymbol.owner
                    if (overridden !in visited) {
                        val functionWithDefaultArguments = dfsImpl(overridden)
                        if (functionWithDefaultArguments != null) {
                            baseFunctionCache[irFunction] = functionWithDefaultArguments
                            return functionWithDefaultArguments
                        }
                    }
                }
            }

            if (irFunction.valueParameters.any { it.defaultValue != null }) {
                baseFunctionCache[irFunction] = irFunction
                return irFunction
            }

            return null
        }

        return dfsImpl(irFunction)
    }

    fun generateDefaultsFunction(
        irFunction: IrFunction,
        context: CommonBackendContext,
        visibility: DescriptorVisibility,
        useConstructorMarker: Boolean,
        copiedAnnotations: List<IrConstructorCall>
    ): IrFunction? {
        if (skipInlineMethods && irFunction.isInline) return null
        if (skipExternalMethods && irFunction.isExternalOrInheritedFromExternal()) return null
        if (context.mapping.defaultArgumentsOriginalFunction[irFunction] != null) return null
        context.mapping.defaultArgumentsDispatchFunction[irFunction]?.let { return it }
        if (irFunction is IrSimpleFunction) {
            // If this is an override of a function with default arguments, produce a fake override of a default stub.
            if (irFunction.overriddenSymbols.any { findBaseFunctionWithDefaultArguments(it.owner) != null })
                return irFunction.generateDefaultsFunctionImpl(
                    context, IrDeclarationOrigin.FAKE_OVERRIDE, visibility, copiedAnnotations, true, useConstructorMarker
                ).also { defaultsFunction ->
                    context.mapping.defaultArgumentsDispatchFunction[irFunction] = defaultsFunction
                    context.mapping.defaultArgumentsOriginalFunction[defaultsFunction] = irFunction

                    if (forceSetOverrideSymbols) {
                        (defaultsFunction as IrSimpleFunction).overriddenSymbols += irFunction.overriddenSymbols.mapNotNull {
                            generateDefaultsFunction(
                                it.owner,
                                context,
                                visibility,
                                useConstructorMarker,
                                it.owner.copyAnnotations()
                            )?.symbol as IrSimpleFunctionSymbol?
                        }
                    }
                }
        }

        // Note: this is intentionally done *after* checking for overrides. While normally `override fun`s
        // have no default parameters, there is an exception in case of interface delegation:
        //     interface I {
        //         fun f(x: Int = 1)
        //     }
        //     class C(val y: I) : I by y {
        //         // implicit `override fun f(x: Int) = y.f(x)` has a default value for `x`
        //     }
        // Since this bug causes the metadata serializer to write the "has default value" flag into compiled
        // binaries, it's way too late to fix it. Hence the workaround.
        if (irFunction.valueParameters.any { it.defaultValue != null }) {
            return irFunction.generateDefaultsFunctionImpl(
                context, IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER, visibility, copiedAnnotations, false, useConstructorMarker
            ).also {
                context.mapping.defaultArgumentsDispatchFunction[irFunction] = it
                context.mapping.defaultArgumentsOriginalFunction[it] = irFunction
            }
        }
        return null
    }

    private fun IrFunction.generateDefaultsFunctionImpl(
        context: CommonBackendContext,
        newOrigin: IrDeclarationOrigin,
        newVisibility: DescriptorVisibility,
        copiedAnnotations: List<IrConstructorCall>,
        isFakeOverride: Boolean,
        useConstructorMarker: Boolean
    ): IrFunction {
        val newFunction = when (this) {
            is IrConstructor ->
                factory.buildConstructor {
                    updateFrom(this@generateDefaultsFunctionImpl)
                    origin = newOrigin
                    isExternal = false
                    isPrimary = false
                    isExpect = false
                    visibility = newVisibility
                }
            is IrSimpleFunction ->
                factory.buildFun {
                    updateFrom(this@generateDefaultsFunctionImpl)
                    name = Name.identifier("${this@generateDefaultsFunctionImpl.name}\$default")
                    origin = newOrigin
                    this.isFakeOverride = isFakeOverride
                    modality = Modality.FINAL
                    isExternal = false
                    isTailrec = false
                    visibility = newVisibility
                }
            else -> throw IllegalStateException("Unknown function type")
        }
        (newFunction as? IrAttributeContainer)?.copyAttributes(this@generateDefaultsFunctionImpl as? IrAttributeContainer)
        newFunction.copyTypeParametersFrom(this)
        newFunction.parent = parent
        newFunction.returnType = returnType.remapTypeParameters(classIfConstructor, newFunction.classIfConstructor)
        newFunction.dispatchReceiverParameter = dispatchReceiverParameter?.copyTo(newFunction)
        newFunction.extensionReceiverParameter = extensionReceiverParameter?.copyTo(newFunction)

        newFunction.valueParameters = valueParameters.map {
            val newType = it.type.remapTypeParameters(classIfConstructor, newFunction.classIfConstructor)
            val makeNullable = it.defaultValue != null &&
                    (context.ir.unfoldInlineClassType(it.type) ?: it.type) !in context.irBuiltIns.primitiveIrTypes
            it.copyTo(
                newFunction,
                type = if (makeNullable) newType.makeNullable() else newType,
                defaultValue = if (it.defaultValue != null) {
                    factory.createExpressionBody(IrErrorExpressionImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, it.type, "Default Stub"))
                } else null,
                isAssignable = it.defaultValue != null
            )
        }

        for (i in 0 until (valueParameters.size + 31) / 32) {
            newFunction.addValueParameter(
                "mask$i".synthesizedString,
                context.irBuiltIns.intType,
                IrDeclarationOrigin.MASK_FOR_DEFAULT_FUNCTION
            )
        }
        if (useConstructorMarker) {
            val markerType = context.ir.symbols.defaultConstructorMarker.defaultType.makeNullable()
            newFunction.addValueParameter("marker".synthesizedString, markerType, IrDeclarationOrigin.DEFAULT_CONSTRUCTOR_MARKER)
        } else if (context.ir.shouldGenerateHandlerParameterForDefaultBodyFun()) {
            newFunction.addValueParameter(
                "handler".synthesizedString,
                context.irBuiltIns.anyNType,
                IrDeclarationOrigin.METHOD_HANDLER_IN_DEFAULT_FUNCTION
            )
        }

        // TODO some annotations are needed (e.g. @JvmStatic), others need different values (e.g. @JvmName), the rest are redundant.
        newFunction.annotations += copiedAnnotations
        return newFunction
    }

}
