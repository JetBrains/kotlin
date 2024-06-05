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
import org.jetbrains.kotlin.backend.wasm.lower.WasmPropertyReferenceLowering.KTypeGeneratorInterface
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl
import org.jetbrains.kotlin.ir.declarations.IrFile
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

    fun createKTypeGenerator(): KTypeGeneratorInterface {
        return KTypeGeneratorInterface { this.irCall(symbols.kTypeStub) }
    }

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

    fun interface KTypeGeneratorInterface {
        fun IrBuilderWithScope.irKType(type: IrType): IrExpression
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

        val kPropertiesField = context.irFactory.stageController.restrictTo(symbols.kProperty1Impl.owner) {
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

                val kTypeGenerator = createKTypeGenerator()
                val startOffset = expression.startOffset
                val endOffset = expression.endOffset
                val irBuilder = context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol, startOffset, endOffset)
                irBuilder.run {
                    val receiversCount = listOf(expression.dispatchReceiver, expression.extensionReceiver).count { it != null }
                    return when (receiversCount) {
                        0 -> { // Cache KProperties with no arguments.
                            val field = kProperties.getOrPut(expression.symbol.owner) {
                                createKProperty(expression, kTypeGenerator, this) to kProperties.size
                            }

                            irCall(arrayItemGetter).apply {
                                dispatchReceiver = irGetField(null, kPropertiesField)
                                putValueArgument(0, irInt(field.second))
                            }
                        }

                        1 -> createKProperty(expression, kTypeGenerator, this) // Has receiver.

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
                    val receiversCount = listOf(expression.dispatchReceiver, expression.extensionReceiver).count { it != null }
                    if (receiversCount == 2)
                        error("Callable reference to properties with two receivers is not allowed: ${expression}")
                    else { // Cache KProperties with no arguments.
                        // TODO: what about `receiversCount == 1` case?
                        val field = kProperties.getOrPut(expression.symbol.owner) {
                            createLocalKProperty(
                                expression.symbol.owner.name.asString(),
                                expression.getter.owner.returnType,
                                createKTypeGenerator(),
                                this
                            ) to kProperties.size
                        }

                        return irCall(arrayItemGetter).apply {
                            dispatchReceiver = irGetField(null, kPropertiesField)
                            putValueArgument(0, irInt(field.second))
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
        kTypeGenerator: KTypeGeneratorInterface,
        irBuilder: IrBuilderWithScope
    ): IrExpression {
        val startOffset = expression.startOffset
        val endOffset = expression.endOffset
        return irBuilder.irBlock(expression) {
            val receiverTypes = mutableListOf<IrType>()
            val dispatchReceiver = expression.dispatchReceiver?.let {
                irTemporary(value = it, nameHint = "\$dispatchReceiver${tempIndex++}")
            }
            val extensionReceiver = expression.extensionReceiver?.let {
                irTemporary(value = it, nameHint = "\$extensionReceiver${tempIndex++}")
            }
            val returnType = expression.getter?.owner?.returnType ?: expression.field!!.owner.type

            val getterCallableReference = expression.getter?.owner?.let { getter ->
                getter.dispatchReceiverParameter.let {
                    if (it != null && expression.dispatchReceiver == null)
                        receiverTypes.add(it.type)
                }
                getter.extensionReceiverParameter.let {
                    if (it != null && expression.extensionReceiver == null)
                        receiverTypes.add(it.type)
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
                    valueArgumentsCount = getter.valueParameters.size,
                    reflectionTarget = expression.getter!!
                ).apply {
                    this.dispatchReceiver = dispatchReceiver?.let { irGet(it) }
                    this.extensionReceiver = extensionReceiver?.let { irGet(it) }
                    for (index in 0 until expression.typeArgumentsCount)
                        putTypeArgument(index, expression.getTypeArgument(index))
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
                        valueArgumentsCount = setter.valueParameters.size,
                        reflectionTarget = expression.setter!!
                    ).apply {
                        this.dispatchReceiver = dispatchReceiver?.let { irGet(it) }
                        this.extensionReceiver = extensionReceiver?.let { irGet(it) }
                        for (index in 0 until expression.typeArgumentsCount)
                            putTypeArgument(index, expression.getTypeArgument(index))
                    }
                }
            }

            val (symbol, constructorTypeArguments) = getKPropertyImplConstructor(
                receiverTypes = receiverTypes,
                returnType = returnType,
                isLocal = false,
                isMutable = setterCallableReference != null
            )

            val initializerType = symbol.owner.returnType.classifierOrFail.typeWith(constructorTypeArguments)
            val initializer = irCall(symbol, initializerType, constructorTypeArguments).apply {
                putValueArgument(0, irString(expression.symbol.owner.name.asString()))
                putValueArgument(1, with(kTypeGenerator) { irKType(returnType) })
                if (getterCallableReference != null)
                    putValueArgument(2, getterCallableReference)
                if (setterCallableReference != null)
                    putValueArgument(3, setterCallableReference)
            }
            +initializer
        }
    }

    private fun createLocalKProperty(
        propertyName: String,
        propertyType: IrType,
        kTypeGenerator: KTypeGeneratorInterface,
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
                putValueArgument(0, irString(propertyName))
                putValueArgument(1, with(kTypeGenerator) { irKType(propertyType) })
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
