/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlock
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.konan.ir.buildSimpleAnnotation
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.descriptors.WrappedFieldDescriptor
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrLocalDelegatedPropertyReference
import org.jetbrains.kotlin.ir.expressions.IrPropertyReference
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

internal class PropertyDelegationLowering(val context: Context) : FileLoweringPass {
    private var tempIndex = 0

    private val kTypeGenerator = KTypeGenerator(context)

    private fun getKPropertyImplConstructor(receiverTypes: List<IrType>,
                                            returnType: IrType,
                                            isLocal: Boolean,
                                            isMutable: Boolean) : Pair<IrConstructorSymbol, List<IrType>> {

        val symbols = context.ir.symbols

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
                        else -> throw AssertionError("More than 2 receivers is not allowed")
                    }
                }

        val arguments = (receiverTypes + listOf(returnType))

        return classSymbol.constructors.single() to arguments
    }

    override fun lower(irFile: IrFile) {
        // Somehow there is no reasonable common ancestor for IrProperty and IrLocalDelegatedProperty,
        // so index by IrDeclaration.
        val kProperties = mutableMapOf<IrDeclaration, Pair<IrExpression, Int>>()

        val arrayClass = context.ir.symbols.array.owner

        val arrayItemGetter = arrayClass.functions.single { it.name == Name.identifier("get") }

        val anyType = context.irBuiltIns.anyType
        val kPropertyImplType = context.ir.symbols.kProperty1Impl.typeWith(anyType, anyType)

        val kPropertiesFieldType: IrType = context.ir.symbols.array.typeWith(kPropertyImplType)

        val kPropertiesField = WrappedFieldDescriptor().let {
            IrFieldImpl(
                    SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
                    DECLARATION_ORIGIN_KPROPERTIES_FOR_DELEGATION,
                    IrFieldSymbolImpl(it),
                    "KPROPERTIES".synthesizedName,
                    kPropertiesFieldType,
                    DescriptorVisibilities.PRIVATE,
                    isFinal = true,
                    isExternal = false,
                    isStatic = true,
            ).apply {
                it.bind(this)
                parent = irFile
                annotations += buildSimpleAnnotation(context.irBuiltIns, startOffset, endOffset, context.ir.symbols.sharedImmutable.owner)
            }
        }

        irFile.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {

            override fun visitPropertyReference(expression: IrPropertyReference): IrExpression {
                expression.transformChildrenVoid(this)

                val startOffset = expression.startOffset
                val endOffset = expression.endOffset
                val irBuilder = context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol, startOffset, endOffset)
                irBuilder.run {
                    val receiversCount = listOf(expression.dispatchReceiver, expression.extensionReceiver).count { it != null }
                    return when (receiversCount) {
                        1 -> createKProperty(expression, this) // Has receiver.

                        2 -> error("Callable reference to properties with two receivers is not allowed: ${expression.symbol.owner.name}")

                        else -> { // Cache KProperties with no arguments.
                            val field = kProperties.getOrPut(expression.symbol.owner) {
                                createKProperty(expression, this) to kProperties.size
                            }

                            irCall(arrayItemGetter).apply {
                                dispatchReceiver = irGetField(null, kPropertiesField)
                                putValueArgument(0, irInt(field.second))
                            }
                        }
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
                        throw AssertionError("Callable reference to properties with two receivers is not allowed: ${expression}")
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
                            dispatchReceiver = irGetField(null, kPropertiesField)
                            putValueArgument(0, irInt(field.second))
                        }
                    }
                }
            }
        })

        if (kProperties.isNotEmpty()) {
            val initializers = kProperties.values.sortedBy { it.second }.map { it.first }
            // TODO: move to object for lazy initialization.
            irFile.declarations.add(0, kPropertiesField.apply {
                initializer = IrExpressionBodyImpl(startOffset, endOffset,
                        context.createArrayOfExpression(startOffset, endOffset, kPropertyImplType, initializers))
            })
        }
    }

    private fun createKProperty(expression: IrPropertyReference, irBuilder: IrBuilderWithScope): IrExpression {
        val startOffset = expression.startOffset
        val endOffset = expression.endOffset
        return irBuilder.irBlock(expression) {
            val receiverTypes = mutableListOf<IrType>()
            val dispatchReceiver = expression.dispatchReceiver.let {
                if (it == null)
                    null
                else
                    irTemporary(value = it, nameHint = "\$dispatchReceiver${tempIndex++}")
            }
            val extensionReceiver = expression.extensionReceiver.let {
                if (it == null)
                    null
                else
                    irTemporary(value = it, nameHint = "\$extensionReceiver${tempIndex++}")
            }
            val returnType = expression.getter?.owner?.returnType ?: expression.field!!.owner.type

            val getterCallableReference = expression.getter?.owner?.let { getter ->
                getter.extensionReceiverParameter.let {
                    if (it != null && expression.extensionReceiver == null)
                        receiverTypes.add(it.type)
                }
                getter.dispatchReceiverParameter.let {
                    if (it != null && expression.dispatchReceiver == null)
                        receiverTypes.add(it.type)
                }
                val getterKFunctionType = this@PropertyDelegationLowering.context.ir.symbols.getKFunctionType(
                        returnType,
                        receiverTypes
                )
                IrFunctionReferenceImpl(
                        startOffset   = startOffset,
                        endOffset     = endOffset,
                        type          = getterKFunctionType,
                        symbol        = expression.getter!!,
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
                    val setterKFunctionType = this@PropertyDelegationLowering.context.ir.symbols.getKFunctionType(
                            context.irBuiltIns.unitType,
                            receiverTypes + returnType
                    )
                    IrFunctionReferenceImpl(
                            startOffset   = startOffset,
                            endOffset     = endOffset,
                            type          = setterKFunctionType,
                            symbol        = expression.setter!!,
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
                    returnType    = returnType,
                    isLocal       = false,
                    isMutable     = setterCallableReference != null)
            val initializer = irCall(symbol.owner, constructorTypeArguments).apply {
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

    private fun createLocalKProperty(propertyName: String,
                                     propertyType: IrType,
                                     irBuilder: IrBuilderWithScope): IrExpression {
        irBuilder.run {
            val (symbol, constructorTypeArguments) = getKPropertyImplConstructor(
                    receiverTypes = emptyList(),
                    returnType = propertyType,
                    isLocal = true,
                    isMutable = false)
            val initializer = irCall(symbol.owner, constructorTypeArguments).apply {
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
            1 -> context.ir.symbols.kMutableProperty0
            2 -> context.ir.symbols.kMutableProperty1
            3 -> context.ir.symbols.kMutableProperty2
            else -> throw AssertionError("More than 2 receivers is not allowed")
        }
        return type.classifier == expectedClass
    }

    private object DECLARATION_ORIGIN_KPROPERTIES_FOR_DELEGATION :
            IrDeclarationOriginImpl("KPROPERTIES_FOR_DELEGATION")
}
