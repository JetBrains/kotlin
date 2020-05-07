/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.plugins.kotlin.frames

import androidx.compose.plugins.kotlin.compiler.lower.COMPOSE_STATEMENT_ORIGIN
import androidx.compose.plugins.kotlin.compiler.lower.ModuleLoweringPass
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptorWithTypeParameters
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorVisitor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.descriptors.impl.ClassConstructorDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.LazyClassReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBlockBodyBuilder
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrAnonymousInitializer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrAnonymousInitializerImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrConstructorImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrTypeParameterImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.expressions.IrSetField
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrAnonymousInitializerSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrConstructorSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrTypeParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.ConstantValueGenerator
import org.jetbrains.kotlin.ir.util.ReferenceSymbolTable
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.referenceFunction
import org.jetbrains.kotlin.ir.util.withScope
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import androidx.compose.plugins.kotlin.frames.analysis.FrameMetadata
import androidx.compose.plugins.kotlin.frames.analysis.FrameWritableSlices
import androidx.compose.plugins.kotlin.frames.analysis.FrameWritableSlices.FRAMED_DESCRIPTOR
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrFieldAccessExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getAllSuperclassesWithoutAny
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.MemberScopeImpl
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.types.ClassTypeConstructorImpl
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.TypeProjection
import org.jetbrains.kotlin.types.TypeSubstitution
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.utils.Printer

/**
 * The frame transformer extension transforms a "framed" classes properties into the form expected
 * by the frames runtime.
 *
 * The transformation is:
 *   - Move the backing fields for public properties from the class itself to a value record.
 *   - Change the property initializers to initialize the value record.
 *   - Change the public property getters and setters to get the current frame record and set or get
 *     the value from that record.
 *   - Remove the moved fields from the class and rewrite any methods that refer to the removed
 *     methods to use the getters and setters instead.
 */
class FrameIrTransformer(val pluginContext: IrPluginContext) :
    IrElementTransformerVoidWithContext(),
    FileLoweringPass,
    ModuleLoweringPass {

    override fun lower(module: IrModuleFragment) {
        module.transformChildrenVoid(this)
    }

    private class FieldRewriteInformation(
        val getter: IrSimpleFunction?,
        val setter: IrSimpleFunction?
    )

    private val fieldRewrites = mutableMapOf<IrFieldSymbol, FieldRewriteInformation>()

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(this)
    }

    override fun visitClassNew(declaration: IrClass): IrStatement {
        val className = declaration.descriptor.fqNameSafe
        val bindingContext = pluginContext.bindingContext
        val symbolTable = pluginContext.symbolTable

        val recordClassDescriptor =
            bindingContext.get(
                FrameWritableSlices.RECORD_CLASS, className
            ) ?: return super.visitClassNew(declaration)

        // If there are no properties, skip the class
        if (!declaration.anyChild { it is IrProperty }) return super.visitClassNew(declaration)

        val framesPackageDescriptor = pluginContext.moduleDescriptor.getPackage(framesPackageName)

        val recordClassScope = recordClassDescriptor.unsubstitutedMemberScope

        val recordTypeDescriptor = pluginContext.moduleDescriptor.findClassAcrossModuleDependencies(
            ClassId.topLevel(recordClassName)
        ) ?: error("Cannot find the Record class")

        val framedDescriptor =
            bindingContext.get(FRAMED_DESCRIPTOR, declaration.descriptor.fqNameSafe)
            ?: error("Could not find framed class descriptor")

        val metadata =
            FrameMetadata(framedDescriptor)

        val fields = mutableListOf<IrField>()
        var recordClass: IrClass? = null

        val framedType =
            pluginContext.moduleDescriptor.findClassAcrossModuleDependencies(
                ClassId.topLevel(framedTypeName)
            ) ?: error("Cannot find the Framed interface")

        val classBuilder = IrClassBuilder(
            pluginContext,
            declaration,
            declaration.descriptor,
            pluginContext.languageVersionSettings
        )

        with(classBuilder) {

            fun toRecord(expr: IrExpression) = syntheticImplicitCast(
                recordTypeDescriptor.defaultType,
                recordClassDescriptor.defaultType.toIrType(),
                expr
            )

            var recordCtorSymbol: IrConstructorSymbol? = null

            // add framed state record
            +innerClass(recordClassDescriptor) { it ->
                recordClass = it
                // Create the state record constructor
                val recordConstructorDescriptor =
                    recordClassDescriptor.unsubstitutedPrimaryConstructor
                    ?: error("Record class constructor not found")
                recordCtorSymbol = symbol(recordConstructorDescriptor)

                +constructor(recordCtorSymbol!!) {
                    // Call the ancestor constructor
                    val superConstructor =
                        symbolTable.referenceConstructor(
                            recordClassDescriptor.getSuperClassNotAny()!!.constructors.single()
                        )

                    // Fields are left uninitialized as they will be set either by the framed object's constructor or by a call to assign()
                    +syntheticConstructorDelegatingCall(
                        superConstructor
                    )
                }

                // Create fields corresponding to the attributes
                val attributes = recordClassScope.getVariableNames()
                for (attribute in attributes) {
                    val member = recordClassScope.getContributedVariables(
                        attribute,
                        NoLookupLocation.FROM_BACKEND
                    ).single()

                    +field(member).also { fields.add(it) }
                }

                val createMethod = recordClassScope
                    .getContributedFunctions(Name.identifier(
                        "create"),
                        NoLookupLocation.FROM_BACKEND
                    ).single()

                +method(createMethod) {
                    // TODO(lmr): this expression seems to cause IntelliJ to freeze analysis. Comment out if you are editing this
                    // file and need analysis to work
                    +irReturn(
                        syntheticConstructorCall(
                            recordClassDescriptor.defaultType.toIrType(),
                            recordCtorSymbol!!
                        )
                    )
                }

                val assignMethod = recordClassScope
                    .getContributedFunctions(
                        Name.identifier("assign"),
                        NoLookupLocation.FROM_BACKEND
                    ).single()

                +method(assignMethod) { irFunction ->
                    val thisParameterReceiver = irFunction.dispatchReceiverParameter
                        ?: error("No receiver found for assign method")
                    val valueParameter = irFunction.valueParameters[0]
                    for (i in 0 until attributes.count()) {
                        val field = fields[i]
                        val thisParameter = syntheticGetValue(
                            thisParameterReceiver.symbol,
                            thisParameterReceiver.type
                        )
                        val other = toRecord(
                            syntheticGetValue(valueParameter.symbol, valueParameter.type)
                        )
                        val otherValue = syntheticGetField(field, other)

                        +syntheticSetField(field, thisParameter, otherValue)
                    }
                }
            }

            // augment this class with the Framed interface
            addInterface(framedType)

            val thisReceiver = declaration.thisReceiver
                ?: error("No this receiver found for class ${declaration.name}")
            val thisSymbol = thisReceiver.symbol
            val thisType = thisReceiver.type
            fun thisValue() = syntheticGetValue(thisSymbol, thisType)

            val recordPropertyDescriptor = PropertyDescriptorImpl.create(
                /* containingDeclaration = */ framedDescriptor,
                /* annotations           = */ Annotations.EMPTY,
                /* modality              = */ Modality.FINAL,
                /* visibility            = */ Visibilities.PRIVATE,
                /* isVar                 = */ true,
                /* name                  = */ Name.identifier("\$record"),
                /* kind                  = */ CallableMemberDescriptor.Kind.DECLARATION,
                /* source                = */ SourceElement.NO_SOURCE,
                /* lateInit              = */ false,
                /* isConst               = */ false,
                /* isExpect              = */ false,
                /* isActual              = */ false,
                /* isExternal            = */ false,
                /* isDelegated           = */ false
            ).apply {
                initialize(null, null)

                setType(
                    recordTypeDescriptor.defaultType,
                    emptyList(),
                    recordTypeDescriptor.thisAsReceiverParameter,
                    null
                )
            }

            val fieldReference = field(recordPropertyDescriptor)

            +fieldReference

            +method(metadata.firstFrameDescriptor(recordTypeDescriptor)) {
                val receiverParameter = it.dispatchReceiverParameter
                    ?: error("Expected receiver for $metadata")
                val dispatchReceiver = syntheticGetValue(
                    receiverParameter.symbol,
                    receiverParameter.type
                )

                // TODO(lmr): this expression seems to cause IntelliJ to freeze analysis. Comment out if you are editing this
                // file and need analysis to work
                +irReturn(syntheticGetField(fieldReference, dispatchReceiver))
            }

            +method(metadata.prependFrameRecordDescriptor(recordTypeDescriptor)) {
                val receiverParameter = it.dispatchReceiverParameter
                    ?: error("Expected reciver for $metadata")
                fun dispatchReceiver() = syntheticGetValue(
                    receiverParameter.symbol,
                    receiverParameter.type
                )

                val nextField =
                    recordTypeDescriptor.unsubstitutedMemberScope.getContributedVariables(
                        Name.identifier("next"),
                        NoLookupLocation.FROM_BACKEND
                    ).singleOrNull() ?: error("Could not find Record.next field")

                val setNextSymbol =
                    symbolTable.referenceFunction(nextField.setter
                        ?: error("Expected setter for Record.next"))
                val valueParameter = it.valueParameters[0]
                fun value() = syntheticGetValue(valueParameter.symbol, valueParameter.type)

                +syntheticSetterCall(
                    setNextSymbol,
                    nextField.setter!!,
                    value(),
                    syntheticGetField(fieldReference, dispatchReceiver())
                )
                +syntheticSetField(
                    fieldReference,
                    dispatchReceiver(),
                    value()
                )
            }

            fun recordGetter(thisSymbol: IrValueSymbol, thisType: IrType) =
                syntheticGetField(fieldReference, syntheticGetValue(thisSymbol, thisType))

            fun getRecord(thisSymbol: IrValueSymbol, thisType: IrType) =
                toRecord(recordGetter(thisSymbol, thisType))

            prepend(initializer {
                // Create the initial state record
                +syntheticSetField(
                    fieldReference, thisValue(), syntheticConstructorCall(
                        recordClassDescriptor.defaultType.toIrType(),
                        recordCtorSymbol!!
                    )
                )

                val unitType: IrType? = context.irBuiltIns.unitType

                // Assign the fields
                metadata.getFramedProperties(bindingContext).forEach { propertyDescriptor ->
                    // Move backing field initializer to an anonymous initializer of the record field
                    val irFramedProperty = declaration.declarations.find {
                        it.descriptor.name == propertyDescriptor.name
                    } as? IrProperty
                        ?: error("Could not find ir representation of ${propertyDescriptor.name}")
                    val irRecordField =
                        fields.find { it.name == propertyDescriptor.name }
                            ?: error("Could not find record field for $className")
                    val backingField = irFramedProperty.backingField
                        ?: TODO("Properties without a backing field are not supported yet")
                    backingField.initializer?.let { initializer ->
                        // (this.next as <record>).<field> = <initializer>
                        +syntheticSetField(
                            irRecordField,
                            getRecord(thisSymbol, thisType),
                            updateDeclarationParents(
                                initializer.expression,
                                backingField,
                                classBuilder.irClass)
                        )
                    }
                }

                // Notify the runtime a new framed object was created
                val createdDescriptor = framesPackageDescriptor.memberScope.getContributedFunctions(
                    Name.identifier("_created"),
                    NoLookupLocation.FROM_BACKEND
                ).single()

                +syntheticCall(
                    unitType!!,
                    symbolTable.referenceSimpleFunction(createdDescriptor)
                ).apply {
                    putValueArgument(0, thisValue())
                }
            })

            // Replace property getter/setters with _readable/_writable calls (this,
            // indirectly, removes the backing field)
            val readableDescriptor =
                framesPackageDescriptor.memberScope.getContributedFunctions(
                    Name.identifier("_readable"),
                    NoLookupLocation.FROM_BACKEND
                ).single()
            val writableDescriptor =
                framesPackageDescriptor.memberScope.getContributedFunctions(
                    Name.identifier("_writable"),
                    NoLookupLocation.FROM_BACKEND
                ).single()
            val readableSymbol = symbolTable.referenceSimpleFunction(readableDescriptor)
            val writableSymbol = symbolTable.referenceSimpleFunction(writableDescriptor)
            metadata.getFramedProperties(
                pluginContext.bindingContext
            ).forEach { propertyDescriptor ->
                val irFramedProperty = declaration.declarations.find {
                    it.descriptor.name == propertyDescriptor.name
                } as? IrProperty
                    ?: error("Could not find ir representation of ${propertyDescriptor.name}")
                val irRecordField = fields.find { it.name == propertyDescriptor.name }
                    ?: error("Could not find record field of ${propertyDescriptor.name}")
                // Properties without backing fields will cause an erro above.
                val backingField = irFramedProperty.backingField!!
                irFramedProperty.backingField = null
                irFramedProperty.getter?.let { getter ->
                    // replace this.field with (_readable(this.next) as <record>).<field>
                    val getterThisReceiver = getter.dispatchReceiverParameter
                        ?: error("No dispatch receiver found for getter")
                    val getterThisSymbol = getterThisReceiver.symbol
                    val getterThisType = getterThisReceiver.type
                    getter.origin = IrDeclarationOrigin.DEFINED
                    getter.body?.transform(object : IrElementTransformer<Nothing?> {
                        override fun visitGetField(
                            expression: IrGetField,
                            data: Nothing?
                        ): IrExpression {
                            val newExpression =
                                if (expression.symbol.descriptor == irFramedProperty.descriptor) {
                                    syntheticGetField(
                                        irRecordField,
                                        toRecord(
                                            syntheticCall(
                                                recordClass!!.defaultType,
                                                readableSymbol
                                            ).also {
                                                it.putValueArgument(
                                                    0,
                                                    recordGetter(
                                                        getterThisSymbol,
                                                        getterThisType
                                                    )
                                                )
                                                it.putValueArgument(
                                                    1,
                                                    syntheticGetValue(
                                                        getterThisSymbol,
                                                        getterThisType
                                                    )
                                                )
                                            }
                                        )
                                    )
                                } else expression
                            return super.visitGetField(newExpression, data)
                        }
                    }, null)
                }

                irFramedProperty.setter?.let { setter ->
                    val setterThisReceiver = setter.dispatchReceiverParameter
                        ?: error("No dispatch receiver found for setter")
                    val setterThisSymbol = setterThisReceiver.symbol
                    val setterThisType = setterThisReceiver.type
                    setter.origin = IrDeclarationOrigin.DEFINED
                    // replace "this.field = value" with "(_writable(this.next) as <record>).<field> = value"
                    setter.body?.transform(object : IrElementTransformer<Nothing?> {
                        override fun visitSetField(
                            expression: IrSetField,
                            data: Nothing?
                        ): IrExpression {
                            val newExpression = if (expression.symbol.descriptor ==
                                irFramedProperty.descriptor) {
                                syntheticSetField(
                                    irRecordField,
                                    toRecord(syntheticCall(
                                        recordClass!!.defaultType,
                                        writableSymbol
                                    ).also {
                                        it.putValueArgument(
                                            0, recordGetter(setterThisSymbol, setterThisType)
                                        )
                                        it.putValueArgument(
                                            1,
                                            syntheticGetValue(setterThisSymbol, setterThisType)
                                        )
                                    }),
                                    expression.value
                                )
                            } else expression
                            return super.visitSetField(newExpression, data)
                        }
                    }, null)
                }

                fieldRewrites[backingField.symbol] = FieldRewriteInformation(
                    irFramedProperty.getter,
                    irFramedProperty.setter
                )
            }
        }

        return super.visitClassNew(classBuilder.irClass)
    }

    override fun visitGetField(expression: IrGetField): IrExpression {
        val getter = fieldRewrites[expression.symbol]?.getter
        if (getter != null) {
            return callAccessor(expression, getter)
        }
        return super.visitGetField(expression)
    }

    override fun visitSetField(expression: IrSetField): IrExpression {
        val setter = fieldRewrites[expression.symbol]?.setter
        if (setter != null) {
            return callAccessor(expression, setter).apply {
                putValueArgument(0, expression.value)
            }
        }
        return super.visitSetField(expression)
    }

    private fun callAccessor(
        expression: IrFieldAccessExpression,
        function: IrSimpleFunction
    ) = IrCallImpl(
        startOffset = expression.startOffset,
        endOffset = expression.endOffset,
        type = expression.type,
        symbol = function.symbol,
        origin = COMPOSE_STATEMENT_ORIGIN
    ).apply {
        dispatchReceiver = expression.receiver
    }
}

fun augmentInterfaceList(
    original: ClassDescriptor,
    addedInterface: ClassDescriptor
): ClassDescriptor =
    object : ClassDescriptor by original {
        override fun getTypeConstructor(): TypeConstructor =
            object : TypeConstructor by original.typeConstructor {
                override fun getSupertypes(): Collection<KotlinType> =
                    original.typeConstructor.supertypes + addedInterface.defaultType
            }
    }

fun augmentClassWithInterface(irClass: IrClass, interfaceDescriptor: ClassDescriptor): IrClass =
    object : IrClass by irClass {
        val newDescriptor by lazy {
            augmentInterfaceList(
                irClass.descriptor,
                interfaceDescriptor
            )
        }

        override val descriptor: ClassDescriptor get() = newDescriptor

        override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
            val result = irClass.accept(visitor, data)
            @Suppress("UNCHECKED_CAST")
            return if (result === irClass) this as R else result
        }

        override fun <D> transform(transformer: IrElementTransformer<D>, data: D): IrStatement {
            val result = irClass.transform(transformer, data)
            if (result == irClass) return this
            return result
        }
    }

// TODO(chuckj): This is copied from ComposeSyntheticExtension. Consider moving it to a location that can be shared.
fun IrElement.find(filter: (descriptor: IrElement) -> Boolean): Collection<IrElement> {
    val elements = mutableListOf<IrElement>()
    accept(object : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) {
            if (filter(element)) elements.add(element)
            element.acceptChildren(this, null)
        }
    }, null)
    return elements
}

fun IrElement.anyChild(filter: (descriptor: IrElement) -> Boolean): Boolean {
    var result = false
    accept(object : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) {
            if (!result) {
                if (filter(element)) {
                    result = true
                } else {
                    element.acceptChildren(this, null)
                }
            }
        }
    }, null)
    return result
}

class IrClassBuilder(
    val pluginContext: IrPluginContext,
    var irClass: IrClass,
    private val classDescriptor: ClassDescriptor,
    val languageVersionSettings: LanguageVersionSettings
) {
    private val typeTranslator =
        TypeTranslator(
            pluginContext.symbolTable,
            languageVersionSettings, pluginContext.builtIns
        ).apply {
            constantValueGenerator =
                ConstantValueGenerator(
                    pluginContext.moduleDescriptor,
                    pluginContext.symbolTable
                )
            constantValueGenerator.typeTranslator = this
        }

    operator fun IrDeclaration.unaryPlus() {
        parent = irClass
        irClass.declarations.add(this)
    }

    fun prepend(declaration: IrDeclaration) {
        declaration.parent = irClass
        irClass.declarations.add(0, declaration)
    }

    fun addInterface(interfaceDescriptor: ClassDescriptor) {
        irClass.superTypes.add(interfaceDescriptor.defaultType.toIrType())
    }

    fun initializer(
        block: IrBlockBodyBuilder.(IrAnonymousInitializer) -> Unit
    ): IrAnonymousInitializer {
        val initializerSymbol = IrAnonymousInitializerSymbolImpl(classDescriptor)
        return IrAnonymousInitializerImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            IrDeclarationOrigin.DELEGATE,
            initializerSymbol
        ).apply {
            body = DeclarationIrBuilder(pluginContext, initializerSymbol).irBlockBody {
                this@irBlockBody.block(this@apply)
            }
        }
    }

    fun innerClass(
        classDescriptor: ClassDescriptor,
        block: IrClassBuilder.(IrClass) -> Unit
    ) = innerClass(
        IrClassSymbolImpl(classDescriptor),
        classDescriptor.thisAsReceiverParameter,
        block
    )

    fun innerClass(
        classSymbol: IrClassSymbol,
        receiverParameterDescriptor: ReceiverParameterDescriptor,
        block: IrClassBuilder.(IrClass) -> Unit
    ) = IrClassImpl(
        UNDEFINED_OFFSET,
        UNDEFINED_OFFSET,
        IrDeclarationOrigin.DELEGATE,
        classSymbol
    ).also {
        it.thisReceiver = syntheticValueParameter(receiverParameterDescriptor).apply {
            parent = it
        }
        it.descriptor.getAllSuperclassesWithoutAny().forEach { superClass ->
            it.superTypes.add(superClass.defaultType.toIrType())
        }
        IrClassBuilder(pluginContext, it, it.descriptor, languageVersionSettings).block(it)
    }

    fun constructor(
        descriptor: ClassConstructorDescriptor,
        block: IrBlockBodyBuilder.(IrConstructor) -> Unit
    ) = constructor(symbol(descriptor), block)

    fun constructor(
        constructorSymbol: IrConstructorSymbol,
        block: IrBlockBodyBuilder.(IrConstructor) -> Unit
    ): IrConstructor {
        return IrConstructorImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            IrDeclarationOrigin.DELEGATE,
            constructorSymbol,
            irClass.defaultType
        ).apply {
            body = DeclarationIrBuilder(pluginContext, constructorSymbol).irBlockBody {
                createParameterDeclarations()
                this@irBlockBody.block(this@apply)
            }
        }
    }

    fun IrFunction.createParameterDeclarations() {
        fun ParameterDescriptor.irValueParameter() = IrValueParameterImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            IrDeclarationOrigin.DEFINED,
            this,
            type.toIrType(),
            (this as? ValueParameterDescriptor)?.varargElementType?.toIrType()
        ).also {
            it.parent = this@createParameterDeclarations
        }

        dispatchReceiverParameter = descriptor.dispatchReceiverParameter?.irValueParameter()
        extensionReceiverParameter = descriptor.extensionReceiverParameter?.irValueParameter()

        assert(valueParameters.isEmpty())
        descriptor.valueParameters.mapTo(valueParameters) { it.irValueParameter() }

        assert(typeParameters.isEmpty())
        descriptor.typeParameters.mapTo(typeParameters) {
            IrTypeParameterImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                IrDeclarationOrigin.DEFINED,
                IrTypeParameterSymbolImpl(it)
            ).also { typeParameter ->
                typeParameter.parent = this
            }
        }
    }

    fun syntheticGetField(field: IrField, receiver: IrExpression) =
        IrGetFieldImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, field.symbol, field.type, receiver)

    fun syntheticSetField(field: IrField, receiver: IrExpression, expression: IrExpression) =
        IrSetFieldImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            field.symbol,
            receiver,
            expression,
            field.type
        )

    fun symbol(member: PropertyDescriptor) = IrFieldSymbolImpl(member)
    fun symbol(method: FunctionDescriptor) = IrSimpleFunctionSymbolImpl(method)
    fun symbol(constructor: ClassConstructorDescriptor) = IrConstructorSymbolImpl(constructor)

    fun field(member: PropertyDescriptor) = IrFieldImpl(
        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
        IrDeclarationOrigin.DELEGATE,
        member,
        member.type.toIrType()
    )

    fun KotlinType.toIrType() = typeTranslator.translateType(this)

    fun method(
        methodDescriptor: FunctionDescriptor,
        returnType: IrType? = null,
        block: IrBlockBodyBuilder.(IrSimpleFunction) -> Unit
    ): IrFunction {
        val realReturnType = returnType ?: methodDescriptor.returnType?.toIrType()

        return IrFunctionImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            IrDeclarationOrigin.DELEGATE,
            methodDescriptor,
            realReturnType ?: pluginContext.irBuiltIns.unitType
        ).apply {
            body = DeclarationIrBuilder(pluginContext, symbol).irBlockBody {
                createParameterDeclarations()
                this@irBlockBody.block(this@apply)
            }
        }
    }

    fun syntheticSetterCall(
        symbol: IrFunctionSymbol,
        descriptor: FunctionDescriptor,
        dispatchReceiver: IrExpression?,
        argument: IrExpression
    ) = IrCallImpl(
        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
        descriptor.returnType!!.toIrType(),
        symbol,
        COMPOSE_STATEMENT_ORIGIN
    ).also {
        it.dispatchReceiver = dispatchReceiver
        it.putValueArgument(0, argument)
    }

    fun syntheticValueParameter(parameter: ParameterDescriptor) = IrValueParameterImpl(
        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
        IrDeclarationOrigin.DELEGATE,
        parameter,
        parameter.type.toIrType(),
        null
    )

    fun syntheticCall(
        kotlinType: IrType,
        symbol: IrFunctionSymbol
    ) =
        IrCallImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            kotlinType,
            symbol,
            typeArgumentsCount = 0
        )

    fun syntheticConstructorCall(
        kotlinType: IrType,
        symbol: IrConstructorSymbol
    ) =
        IrConstructorCallImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            kotlinType,
            symbol,
            typeArgumentsCount = 0,
            constructorTypeArgumentsCount = 0,
            valueArgumentsCount = 0
        )

    fun syntheticGetValue(symbol: IrValueSymbol, type: IrType) =
        IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, symbol)

    fun syntheticImplicitCast(
        fromType: KotlinType,
        toType: IrType,
        argument: IrExpression
    ) =
        IrTypeOperatorCallImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET, toType, IrTypeOperator.IMPLICIT_CAST,
            fromType.toIrType(), argument
        )
}

fun IrBlockBodyBuilder.syntheticConstructorDelegatingCall(
    symbol: IrConstructorSymbol
) = IrDelegatingConstructorCallImpl(
        UNDEFINED_OFFSET,
        UNDEFINED_OFFSET,
        context.irBuiltIns.unitType,
        symbol,
        typeArgumentsCount = 0
    )

inline fun <T : IrDeclaration> T.buildWithScope(
    context: GeneratorContext,
    crossinline builder: (T) -> Unit
): T =
    also { irDeclaration ->
        context.symbolTable.withScope(irDeclaration.descriptor) {
            builder(irDeclaration)
        }
    }

inline fun <T, D : DeclarationDescriptor> ReferenceSymbolTable.withScope(
    owner: D,
    block: ReferenceSymbolTable.(D) -> T
): T {
    enterScope(owner)
    val result = block(owner)
    leaveScope(owner)
    return result
}

val IrClass.containingFile: IrFile?
    get() {
        var node: IrDeclarationParent? = parent
        while (node != null) {
            if (node is IrFile) return node
            node = (node as? IrDeclaration)?.parent
        }
        return null
    }

class SyntheticFramePackageDescriptor(
    module: ModuleDescriptor,
    fqName: FqName
) : PackageFragmentDescriptorImpl(module, fqName) {
    private lateinit var classDescriptor: ClassDescriptor
    private val scope: MemberScope = object : MemberScopeImpl() {
        override fun getContributedDescriptors(
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean
        ): List<DeclarationDescriptor> =
            if (nameFilter(classDescriptor.name)) listOf(classDescriptor) else emptyList()

        override fun getContributedClassifier(
            name: Name,
            location: LookupLocation
        ): ClassifierDescriptor? =
            if (classDescriptor.fqNameSafe ==
                FqName(fqName.toString() + "" + name.identifier)) classDescriptor else null

        override fun printScopeStructure(p: Printer) {
            p.println(this::class.java.simpleName)
        }
    }

    override fun getMemberScope() = scope

    fun setClassDescriptor(value: ClassDescriptor) {
        classDescriptor = value
    }
}

private fun updateDeclarationParents(
    expression: IrExpression,
    oldParent: IrDeclarationParent,
    newParent: IrDeclarationParent
): IrExpression {
    expression.transformChildrenVoid(object : IrElementTransformerVoid() {
        override fun visitDeclaration(declaration: IrDeclaration): IrStatement {
            if (declaration.parent == oldParent) {
                declaration.parent = newParent
            }
            return super.visitDeclaration(declaration)
        }
    })
    return expression
}

// TODO(chuckj): Consider refactoring to have a shared synthetic base with other synthetic classes
class FrameRecordClassDescriptor(
    private val myName: Name,
    private val myContainingDeclaration: DeclarationDescriptor,
    val recordDescriptor: ClassDescriptor,
    myFramedClassDescriptor: ClassDescriptor,
    mySuperTypes: Collection<KotlinType>,
    bindingContext: BindingContext
) : ClassDescriptor {
    override fun getKind() = ClassKind.CLASS
    override fun getDefaultFunctionTypeForSamInterface(): SimpleType? = null

    override fun getModality() = Modality.FINAL
    override fun getName() = myName
    override fun getSource() = SourceElement.NO_SOURCE!!
    override fun getMemberScope(typeArguments: MutableList<out TypeProjection>): MemberScope =
        myScope
    override fun getMemberScope(typeSubstitution: TypeSubstitution): MemberScope = myScope
    override fun getUnsubstitutedMemberScope(): MemberScope = myScope
    override fun getUnsubstitutedInnerClassesScope(): MemberScope = myScope
    override fun getStaticScope(): MemberScope = myScope
    override fun getConstructors(): Collection<ClassConstructorDescriptor> =
        listOf(myUnsubstitutedPrimaryConstructor)
    override fun getContainingDeclaration() = myContainingDeclaration
    override fun getDefaultType() = myDefaultType
    override fun getCompanionObjectDescriptor(): ClassDescriptor? = null
    override fun getVisibility() = Visibilities.PUBLIC
    override fun isCompanionObject() = false
    override fun isData(): Boolean = false
    override fun isInline() = false
    override fun getThisAsReceiverParameter() = thisAsReceiverParameter
    override fun getUnsubstitutedPrimaryConstructor(): ClassConstructorDescriptor? =
        myUnsubstitutedPrimaryConstructor

    override fun isFun(): Boolean = false

    override fun getSealedSubclasses(): Collection<ClassDescriptor> = emptyList()
    override fun getOriginal(): ClassDescriptor = this
    override fun isExpect() = false
    override fun substitute(substitutor: TypeSubstitutor): ClassifierDescriptorWithTypeParameters =
        error("Class $this can't be substituted")

    override fun isActual(): Boolean = false
    override fun getTypeConstructor() = myTypeConstructor
    override fun isInner() = false

    override fun <R : Any?, D : Any?> accept(
        visitor: DeclarationDescriptorVisitor<R, D>?,
        data: D
    ): R {
        return visitor!!.visitClassDescriptor(this, data)
    }

    override fun getDeclaredTypeParameters() = emptyList<TypeParameterDescriptor>()

    override fun isExternal() = false

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>?) {
        visitor?.visitClassDescriptor(this, null)
    }

    override fun isDefinitelyNotSamInterface(): Boolean = true

    override val annotations = Annotations.EMPTY

    private val myUnsubstitutedPrimaryConstructor: ClassConstructorDescriptor by lazy {
        val constructor = ClassConstructorDescriptorImpl.create(
            this,
            Annotations.EMPTY,
            false,
            SourceElement.NO_SOURCE
        )
        constructor.initialize(
            emptyList(),
            Visibilities.PUBLIC
        )

        constructor.apply {
            returnType = containingDeclaration.defaultType
        }
        constructor as ClassConstructorDescriptor
    }

    private val myScope = genScope(bindingContext)

    private val myDefaultType: SimpleType by lazy {
        TypeUtils.makeUnsubstitutedType(this, unsubstitutedMemberScope,
            KotlinTypeFactory.EMPTY_REFINED_TYPE_FACTORY)
    }

    private val thisAsReceiverParameter = LazyClassReceiverParameterDescriptor(this)
    private val myTypeConstructor =
        ClassTypeConstructorImpl(
            this,
            declaredTypeParameters,
            mySuperTypes,
            LockBasedStorageManager.NO_LOCKS
        )
    private val myMetadata =
        FrameMetadata(myFramedClassDescriptor)

    private fun genScope(bindingContext: BindingContext): MemberScope {
        return object : MemberScopeImpl() {

            override fun printScopeStructure(p: Printer) {
                p.println(this::class.java.simpleName)
            }

            override fun getVariableNames() = myVariableNames
            override fun getContributedVariables(
                name: Name,
                location: LookupLocation
            ): Collection<PropertyDescriptor> =
                myContributedVariables[name]?.let { listOf(it) } ?: emptyList()

            override fun getFunctionNames(): Set<Name> = myFunctionNames
            override fun getContributedFunctions(
                name: Name,
                location: LookupLocation
            ): Collection<SimpleFunctionDescriptor> =
                myContributedFunctions[name]?.let { listOf(it) } ?: emptyList()

            val myVariableNames by lazy { myContributedVariables.keys }
            val myContributedVariables by lazy {
                myMetadata.getRecordPropertyDescriptors(
                    this@FrameRecordClassDescriptor,
                    bindingContext
                ).map {
                    it.name to it
                }.toMap()
            }
            val myFunctionNames by lazy { myContributedFunctions.keys }
            val myContributedFunctions: Map<Name, SimpleFunctionDescriptor> by lazy {
                myMetadata.getRecordMethodDescriptors(
                    this@FrameRecordClassDescriptor,
                    recordDescriptor
                ).map {
                    it.name to it
                }.toMap()
            }
        }
    }
}
