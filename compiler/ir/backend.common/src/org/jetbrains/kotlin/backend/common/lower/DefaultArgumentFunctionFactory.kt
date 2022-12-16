/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.buildConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrErrorExpressionImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name

abstract class DefaultArgumentFunctionFactory(open val context: CommonBackendContext) {
    protected fun IrFunction.generateDefaultArgumentsFunctionName() =
        Name.identifier("${name}\$default")

    protected abstract fun IrFunction.generateDefaultArgumentStubFrom(original: IrFunction, useConstructorMarker: Boolean)

    protected fun IrFunction.copyAttributesFrom(original: IrFunction) {
        (this as? IrAttributeContainer)?.copyAttributes(original as? IrAttributeContainer)
    }

    protected fun IrFunction.copyReturnTypeFrom(original: IrFunction) {
        returnType = original.returnType.remapTypeParameters(original.classIfConstructor, classIfConstructor)
    }

    protected fun IrFunction.copyReceiversFrom(original: IrFunction) {
        dispatchReceiverParameter = original.dispatchReceiverParameter?.copyTo(this)
        extensionReceiverParameter = original.extensionReceiverParameter?.copyTo(this)
        contextReceiverParametersCount = original.contextReceiverParametersCount
    }

    protected fun IrFunction.copyValueParametersFrom(original: IrFunction, wrapWithNullable: Boolean = true) {
        valueParameters = original.valueParameters.map {
            val newType = it.type.remapTypeParameters(original.classIfConstructor, classIfConstructor)
            val makeNullable = wrapWithNullable && it.defaultValue != null &&
                    (context.ir.unfoldInlineClassType(it.type) ?: it.type) !in context.irBuiltIns.primitiveIrTypes
            it.copyTo(
                this,
                type = if (makeNullable) newType.makeNullable() else newType,
                defaultValue = if (it.defaultValue != null) {
                    original.factory.createExpressionBody(
                        IrErrorExpressionImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, it.type, "Default Stub").apply {
                            attributeOwnerId = it.defaultValue!!.expression
                        }
                    )
                } else null,
                isAssignable = it.defaultValue != null
            )
        }
    }

    fun findBaseFunctionWithDefaultArgumentsFor(
        declaration: IrFunction,
        skipInlineMethods: Boolean,
        skipExternalMethods: Boolean
    ): IrFunction? {
        val visited = mutableSetOf<IrFunction>()

        fun IrFunction.dfsImpl(): IrFunction? {
            visited += this

            if (isInline && skipInlineMethods) return null
            if (skipExternalMethods && isExternalOrInheritedFromExternal()) return null

            if (this is IrSimpleFunction) {
                overriddenSymbols.forEach { overridden ->
                    val base = overridden.owner
                    if (base !in visited) base.dfsImpl()?.let { return it }
                }
            }

            if (valueParameters.any { it.defaultValue != null }) return this

            return null
        }
        return declaration.dfsImpl()
    }


    fun generateDefaultsFunction(
        declaration: IrFunction,
        skipInlineMethods: Boolean,
        skipExternalMethods: Boolean,
        forceSetOverrideSymbols: Boolean,
        visibility: DescriptorVisibility,
        useConstructorMarker: Boolean,
        copiedAnnotations: List<IrConstructorCall>,
    ): IrFunction? {
        if (skipInlineMethods && declaration.isInline) return null
        if (skipExternalMethods && declaration.isExternalOrInheritedFromExternal()) return null
        if (context.mapping.defaultArgumentsOriginalFunction[declaration] != null) return null
        context.mapping.defaultArgumentsDispatchFunction[declaration]?.let { return it }
        if (declaration is IrSimpleFunction) {
            // If this is an override of a function with default arguments, produce a fake override of a default stub.
            if (declaration.overriddenSymbols.any {
                    findBaseFunctionWithDefaultArgumentsFor(
                        it.owner,
                        skipInlineMethods,
                        skipExternalMethods
                    ) != null
                })
                return generateDefaultsFunctionImpl(
                    declaration,
                    IrDeclarationOrigin.FAKE_OVERRIDE,
                    visibility,
                    copiedAnnotations,
                    true,
                    useConstructorMarker,
                ).also { defaultsFunction ->
                    context.mapping.defaultArgumentsDispatchFunction[declaration] = defaultsFunction
                    context.mapping.defaultArgumentsOriginalFunction[defaultsFunction] = declaration

                    if (forceSetOverrideSymbols) {
                        (defaultsFunction as IrSimpleFunction).overriddenSymbols += declaration.overriddenSymbols.mapNotNull {
                            generateDefaultsFunction(
                                it.owner,
                                skipInlineMethods,
                                skipExternalMethods,
                                forceSetOverrideSymbols,
                                visibility,
                                useConstructorMarker,
                                it.owner.copyAnnotations(),
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
        if (declaration.valueParameters.any { it.defaultValue != null }) {
            return generateDefaultsFunctionImpl(
                declaration,
                IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER,
                visibility,
                copiedAnnotations,
                false,
                useConstructorMarker,
            ).also {
                context.mapping.defaultArgumentsDispatchFunction[declaration] = it
                context.mapping.defaultArgumentsOriginalFunction[it] = declaration
            }
        }
        return null
    }


    private fun generateDefaultsFunctionImpl(
        declaration: IrFunction,
        newOrigin: IrDeclarationOrigin,
        newVisibility: DescriptorVisibility,
        copiedAnnotations: List<IrConstructorCall>,
        isFakeOverride: Boolean,
        useConstructorMarker: Boolean,
    ): IrFunction {
        val newFunction = when (declaration) {
            is IrConstructor ->
                declaration.factory.buildConstructor {
                    updateFrom(declaration)
                    origin = newOrigin
                    isExternal = false
                    isPrimary = false
                    isExpect = false
                    visibility = newVisibility
                }

            is IrSimpleFunction ->
                declaration.factory.buildFun {
                    updateFrom(declaration)
                    name = declaration.generateDefaultArgumentsFunctionName()
                    origin = newOrigin
                    this.isFakeOverride = isFakeOverride
                    modality = Modality.FINAL
                    isExternal = false
                    isTailrec = false
                    visibility = newVisibility
                }

            else -> throw IllegalStateException("Unknown function type")
        }

        return newFunction.apply {
            parent = declaration.parent
            generateDefaultArgumentStubFrom(declaration, useConstructorMarker)
            // TODO some annotations are needed (e.g. @JvmStatic), others need different values (e.g. @JvmName), the rest are redundant.
            annotations += copiedAnnotations
        }
    }
}
