/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ir.createArrayOfExpression
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlock
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrLocalDelegatedPropertyReference
import org.jetbrains.kotlin.ir.expressions.IrPropertyReference
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name


internal class WasmPropertyReferenceLowering(val context: WasmBackendContext) : FileLoweringPass {
    private var tempIndex = 0
    val symbols = context.wasmSymbols

    private fun getKPropertyImplConstructor(
        receiverTypes: List<IrType>,
        returnType: IrType,
        isLocal: Boolean,
        isMutable: Boolean
    ): Pair<IrConstructorSymbol, List<IrType>> {


        val classSymbol =
            if (isLocal) {
                assert(receiverTypes.isEmpty()) { "Local delegated property cannot have explicit receiver" }
                when {
                    isMutable -> symbols.kLocalDelegatedMutablePropertyImpl
                    else -> symbols.kLocalDelegatedPropertyImpl
                }
            } else {
                when (receiverTypes.size) {
                    0 -> when {
                        isMutable -> symbols.kMutableProperty0Impl
                        else -> symbols.kProperty0Impl
                    }
                    1 -> when {
                        isMutable -> symbols.kMutableProperty1Impl
                        else -> symbols.kProperty1Impl
                    }
                    2 -> when {
                        isMutable -> symbols.kMutableProperty2Impl
                        else -> symbols.kProperty2Impl
                    }
                    else -> error("More than 2 receivers is not allowed")
                }
            }

        val arguments = (receiverTypes + listOf(returnType))

        return classSymbol.constructors.single() to arguments
    }

    override fun lower(irFile: IrFile) {
        // Somehow there is no reasonable common ancestor for IrProperty and IrLocalDelegatedProperty,
        // so index by IrDeclaration.
        val kProperties = mutableMapOf<IrDeclaration, Pair<IrExpression, Int>>()

        val arrayClass = context.irBuiltIns.arrayClass.owner

        val arrayItemGetter = arrayClass.functions.single { it.name == Name.identifier("get") }

        val anyType = context.irBuiltIns.anyType
        val kPropertyImplType = symbols.kProperty1Impl.typeWith(anyType, anyType)

        val kPropertiesFieldType: IrType = arrayClass.typeWith(kPropertyImplType)

        val firstFileDeclaration = irFile.declarations.firstOrNull() ?: return

        //TODO Check is this valid to use firstFileDeclaration as restrict
        val kPropertiesField = context.irFactory.stageController.restrictTo(firstFileDeclaration) {
            context.irFactory.createField(
                startOffset = SYNTHETIC_OFFSET,
                endOffset = SYNTHETIC_OFFSET,
                origin = DECLARATION_ORIGIN_KPROPERTIES_FOR_DELEGATION,
                name = Name.identifier("\$KPROPERTIES"),
                visibility = DescriptorVisibilities.PRIVATE,
                symbol = IrFieldSymbolImpl(),
                type = kPropertiesFieldType,
                isFinal = true,
                isStatic = true,
                isExternal = false,
            ).apply {
                parent = irFile
            }
        }

        irFile.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {

            override fun visitPropertyReference(expression: IrPropertyReference): IrExpression {
                expression.transformChildrenVoid(this)

                val startOffset = expression.startOffset
                val endOffset = expression.endOffset
                val irBuilder = context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol, startOffset, endOffset)
                irBuilder.run {
                    val receiversCount = expression.arguments.count { it != null }
                    return when (receiversCount) {
                        0 -> { // Cache KProperties with no arguments.
                            val field = kProperties.getOrPut(expression.symbol.owner) {
                                createKProperty(expression, this) to kProperties.size
                            }

                            irCall(arrayItemGetter).apply {
                                arguments[0] = irGetField(null, kPropertiesField)
                                arguments[1] = irInt(field.second)
                            }
                        }

                        1 -> createKProperty(expression, this) // Has receiver.

                        else -> error("Callable reference to properties with two receivers is not allowed: ${expression.symbol.owner.name}")
                    }
                }
            }

            override fun visitLocalDelegatedPropertyReference(expression: IrLocalDelegatedPropertyReference): IrExpression {
                expression.transformChildrenVoid(this)

                val startOffset = expression.startOffset
                val endOffset = expression.endOffset
                val irBuilder = context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol, startOffset, endOffset)
                irBuilder.run {
                    val receiversCount = expression.getter.owner.parameters
                        .count { it.kind != IrParameterKind.Regular && it.kind != IrParameterKind.Context }

                    if (receiversCount == 2)
                        error("Callable reference to properties with two receivers is not allowed: ${expression}")
                    else { // Cache KProperties with no arguments.
                        // TODO: what about `receiversCount == 1` case?
                        val field = kProperties.getOrPut(expression.symbol.owner) {
                            createLocalKProperty(
                                expression.symbol.owner.name.asString(),
                                expression.getter.owner.returnType,
                                this
                            ) to kProperties.size
                        }

                        return irCall(arrayItemGetter).apply {
                            arguments[0] = irGetField(null, kPropertiesField)
                            arguments[1] = irInt(field.second)
                        }
                    }
                }
            }
        })

        if (kProperties.isNotEmpty()) {
            val initializers = kProperties.values.sortedBy { it.second }.map { it.first }
            // TODO: replace with static initialization.
            kPropertiesField.initializer = context.irFactory.createExpressionBody(
                SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
                context.createArrayOfExpression(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, kPropertyImplType, initializers)
            )
            irFile.declarations.add(0, kPropertiesField)
        }
    }

    private fun createKProperty(
        expression: IrPropertyReference,
        irBuilder: IrBuilderWithScope
    ): IrExpression {
        val startOffset = expression.startOffset
        val endOffset = expression.endOffset
        return irBuilder.irBlock(expression) {
            val receiverTypes = mutableListOf<IrType>()
            val temporaries = expression.arguments.map { argument ->
                argument?.let {
                    irTemporary(value = it, nameHint = "\$KPropertyArgument${tempIndex++}")
                }
            }

            val returnType = expression.getter?.owner?.returnType ?: expression.field!!.owner.type

            val getterCallableReference = expression.getter?.owner?.let { getter ->
                getter.parameters.zip(temporaries).forEach { (parameter, argument) ->
                    if (argument == null) {
                        receiverTypes.add(parameter.type)
                    }
                }
                val getterKFunctionType = this@WasmPropertyReferenceLowering.context.irBuiltIns.getKFunctionType(
                    returnType,
                    receiverTypes
                )
                IrFunctionReferenceImpl(
                    startOffset = startOffset,
                    endOffset = endOffset,
                    type = getterKFunctionType,
                    symbol = expression.getter!!,
                    typeArgumentsCount = getter.typeParameters.size,
                    reflectionTarget = expression.getter!!
                ).apply {
                    temporaries.forEachIndexed { index, argument ->
                        arguments[index] = argument?.let { irGet(it) }
                    }
                    for (index in expression.typeArguments.indices) {
                        typeArguments[index] = expression.typeArguments[index]
                    }
                }
            }

            val setterCallableReference = expression.setter?.owner?.let { setter ->
                if (!isKMutablePropertyType(expression.type)) null
                else {
                    val setterKFunctionType = this@WasmPropertyReferenceLowering.context.irBuiltIns.getKFunctionType(
                        context.irBuiltIns.unitType,
                        receiverTypes + returnType
                    )
                    IrFunctionReferenceImpl(
                        startOffset = startOffset,
                        endOffset = endOffset,
                        type = setterKFunctionType,
                        symbol = expression.setter!!,
                        typeArgumentsCount = setter.typeParameters.size,
                        reflectionTarget = expression.setter!!
                    ).apply {
                        temporaries.forEachIndexed { index, argument ->
                            this.arguments[index] = argument?.let { irGet(it) }
                        }
                        for (index in expression.typeArguments.indices) {
                            typeArguments[index] = expression.typeArguments[index]
                        }
                    }
                }
            }

            val (symbol, constructorTypeArguments) = getKPropertyImplConstructor(
                receiverTypes = receiverTypes,
                returnType = returnType,
                isLocal = false,
                isMutable = setterCallableReference != null
            )

            val capturedReceiver = expression.dispatchReceiver != null || expression.dispatchReceiver != null

            val initializerType = symbol.owner.returnType.classifierOrFail.typeWith(constructorTypeArguments)
            val initializer = irCall(symbol, initializerType, constructorTypeArguments).apply {
                arguments[0] = irString(expression.symbol.owner.name.asString())
                arguments[1] = irString(expression.symbol.owner.parent.kotlinFqName.asString())
                arguments[2] = irBoolean(capturedReceiver)
                if (getterCallableReference != null)
                    arguments[3] = getterCallableReference
                if (setterCallableReference != null)
                    arguments[4] = setterCallableReference
            }
            +initializer
        }
    }

    private fun createLocalKProperty(
        propertyName: String,
        propertyType: IrType,
        irBuilder: IrBuilderWithScope
    ): IrExpression {
        irBuilder.run {
            val (symbol, constructorTypeArguments) = getKPropertyImplConstructor(
                receiverTypes = emptyList(),
                returnType = propertyType,
                isLocal = true,
                isMutable = false
            )
            val initializerType = symbol.owner.returnType.classifierOrFail.typeWith(constructorTypeArguments)
            val initializer = irCall(symbol, initializerType, constructorTypeArguments).apply {
                arguments[0] = irString(propertyName)
            }
            return initializer
        }
    }

    private fun isKMutablePropertyType(type: IrType): Boolean {
        if (type !is IrSimpleType) return false
        val expectedClass = when (type.arguments.size) {
            0 -> return false
            1 -> symbols.kMutableProperty0
            2 -> symbols.kMutableProperty1
            3 -> symbols.kMutableProperty2
            else -> error("More than 2 receivers is not allowed")
        }
        return type.classifier == expectedClass
    }

    companion object {
        val DECLARATION_ORIGIN_KPROPERTIES_FOR_DELEGATION = IrDeclarationOriginImpl("KPROPERTIES_FOR_DELEGATION")
    }
}
