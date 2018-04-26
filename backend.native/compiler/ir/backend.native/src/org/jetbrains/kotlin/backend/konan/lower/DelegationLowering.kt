/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlock
import org.jetbrains.kotlin.backend.konan.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.konan.KonanBackendContext
import org.jetbrains.kotlin.backend.konan.irasdescriptors.typeWith
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrLocalDelegatedProperty
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrLocalDelegatedPropertyReference
import org.jetbrains.kotlin.ir.expressions.IrPropertyReference
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.replace

internal class PropertyDelegationLowering(val context: KonanBackendContext) : FileLoweringPass {
    private val reflectionTypes = context.reflectionTypes
    private var tempIndex = 0

    private fun getKPropertyImplConstructor(receiverTypes: List<IrType>,
                                            returnType: IrType,
                                            isLocal: Boolean,
                                            isMutable: Boolean) : Pair<IrConstructorSymbol, List<IrType>> {

        val symbols = context.ir.symbols

        val classSymbol =
                if (isLocal) {
                    assert(receiverTypes.isEmpty(), { "Local delegated property cannot have explicit receiver" })
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

    private fun ClassDescriptor.replace(vararg type: KotlinType): SimpleType {
        return this.defaultType.replace(type.map(::TypeProjectionImpl))
    }

    override fun lower(irFile: IrFile) {
        val kProperties = mutableMapOf<VariableDescriptorWithAccessors, Pair<IrExpression, Int>>()

        val arrayClass = context.ir.symbols.array.owner

        val arrayItemGetter = arrayClass.functions.single { it.descriptor.name == Name.identifier("get") }

        val anyType = context.irBuiltIns.anyType
        val kPropertyImplType = context.ir.symbols.kProperty1Impl.typeWith(anyType, anyType)

        val kPropertiesFieldType: IrType = context.ir.symbols.array.typeWith(kPropertyImplType)

        val kPropertiesField = IrFieldImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                DECLARATION_ORIGIN_KPROPERTIES_FOR_DELEGATION,
                createKPropertiesFieldDescriptor(irFile.packageFragmentDescriptor,
                        kPropertiesFieldType.toKotlinType()
                ),
                kPropertiesFieldType
        )

        irFile.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {

            override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty): IrStatement {
                declaration.transformChildrenVoid(this)

                val initializer = declaration.delegate.initializer!!
                declaration.delegate.initializer = IrBlockImpl(initializer.startOffset, initializer.endOffset, initializer.type, null,
                        listOf(
                                declaration.getter,
                                declaration.setter,
                                initializer
                        ).filterNotNull())

                return declaration.delegate
            }

            override fun visitPropertyReference(expression: IrPropertyReference): IrExpression {
                expression.transformChildrenVoid(this)

                val startOffset = expression.startOffset
                val endOffset = expression.endOffset
                val irBuilder = context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol, startOffset, endOffset)
                irBuilder.run {
                    val receiversCount = listOf(expression.dispatchReceiver, expression.extensionReceiver).count { it != null }
                    if (receiversCount == 1) // Has receiver.
                        return createKProperty(expression, this)
                    else if (receiversCount == 2)
                        throw AssertionError("Callable reference to properties with two receivers is not allowed: ${expression.descriptor}")
                    else { // Cache KProperties with no arguments.
                        val field = kProperties.getOrPut(expression.descriptor) {
                            createKProperty(expression, this) to kProperties.size
                        }

                        return irCall(arrayItemGetter).apply {
                            dispatchReceiver = irGetField(null, kPropertiesField)
                            putValueArgument(0, irInt(field.second))
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
                    val propertyDescriptor = expression.descriptor

                    val receiversCount = listOf(expression.dispatchReceiver, expression.extensionReceiver).count { it != null }
                    if (receiversCount == 2)
                        throw AssertionError("Callable reference to properties with two receivers is not allowed: $propertyDescriptor")
                    else { // Cache KProperties with no arguments.
                        // TODO: what about `receiversCount == 1` case?
                        val field = kProperties.getOrPut(propertyDescriptor) {
                            createLocalKProperty(
                                    propertyDescriptor,
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
                initializer = IrExpressionBodyImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                        context.createArrayOfExpression(kPropertyImplType, initializers, UNDEFINED_OFFSET, UNDEFINED_OFFSET))
            })

            kPropertiesField.parent = irFile
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
            val propertyDescriptor = expression.descriptor
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
                        descriptor    = getter.descriptor,
                        typeArgumentsCount = 0
                ).apply {
                    this.dispatchReceiver = dispatchReceiver?.let { irGet(it) }
                    this.extensionReceiver = extensionReceiver?.let { irGet(it) }
                }
            }

            val setterCallableReference = expression.setter?.owner?.let {
                if (!isKMutablePropertyType(expression.type.toKotlinType())) null
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
                            descriptor    = it.descriptor,
                            typeArgumentsCount = 0
                    ).apply {
                        this.dispatchReceiver = dispatchReceiver?.let { irGet(it) }
                        this.extensionReceiver = extensionReceiver?.let { irGet(it) }
                    }
                }
            }

            val (symbol, constructorTypeArguments) = getKPropertyImplConstructor(
                    receiverTypes = receiverTypes,
                    returnType    = returnType,
                    isLocal       = false,
                    isMutable     = setterCallableReference != null)
            val initializer = irCall(symbol.owner, constructorTypeArguments).apply {
                putValueArgument(0, irString(propertyDescriptor.name.asString()))
                if (getterCallableReference != null)
                    putValueArgument(1, getterCallableReference)
                if (setterCallableReference != null)
                    putValueArgument(2, setterCallableReference)
            }
            +initializer
        }
    }

    private fun createLocalKProperty(propertyDescriptor: VariableDescriptorWithAccessors,
                                     propertyType: IrType,
                                     irBuilder: IrBuilderWithScope): IrExpression {
        irBuilder.run {
            val (symbol, constructorTypeArguments) = getKPropertyImplConstructor(
                    receiverTypes = emptyList(),
                    returnType = propertyType,
                    isLocal = true,
                    isMutable = false)
            val initializer = irCall(symbol.owner, constructorTypeArguments).apply {
                putValueArgument(0, irString(propertyDescriptor.name.asString()))
            }
            return initializer
        }
    }

    private fun isKMutablePropertyType(type: KotlinType): Boolean {
        val arguments = type.arguments
        val expectedClassDescriptor = when (arguments.size) {
            0 -> return false
            1 -> reflectionTypes.kMutableProperty0
            2 -> reflectionTypes.kMutableProperty1
            3 -> reflectionTypes.kMutableProperty2
            else -> throw AssertionError("More than 2 receivers is not allowed")
        }
        return type == expectedClassDescriptor.defaultType.replace(arguments)
    }

    private object DECLARATION_ORIGIN_KPROPERTIES_FOR_DELEGATION :
            IrDeclarationOriginImpl("KPROPERTIES_FOR_DELEGATION")

    private fun createKPropertiesFieldDescriptor(containingDeclaration: DeclarationDescriptor, fieldType: KotlinType): PropertyDescriptorImpl {
        return PropertyDescriptorImpl.create(containingDeclaration, Annotations.EMPTY, Modality.FINAL, Visibilities.PRIVATE,
                false, "KPROPERTIES".synthesizedName, CallableMemberDescriptor.Kind.SYNTHESIZED, SourceElement.NO_SOURCE,
                false, false, false, false, false, false).apply {

            val receiverType: KotlinType? = null
            this.setType(fieldType, emptyList(), null, receiverType)
            this.initialize(null, null)
        }
    }
}

