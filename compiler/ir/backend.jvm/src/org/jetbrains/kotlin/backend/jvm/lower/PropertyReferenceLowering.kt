/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ir.addSimpleDelegatingConstructor
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.ir.createJvmIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.irArrayOf
import org.jetbrains.kotlin.backend.jvm.ir.needsAccessor
import org.jetbrains.kotlin.backend.jvm.lower.FunctionReferenceLowering.Companion.calculateOwner
import org.jetbrains.kotlin.backend.jvm.lower.FunctionReferenceLowering.Companion.calculateOwnerKClass
import org.jetbrains.kotlin.backend.jvm.lower.FunctionReferenceLowering.Companion.kClassToJavaClass
import org.jetbrains.kotlin.backend.jvm.lower.inlineclasses.InlineClassAbi
import org.jetbrains.kotlin.backend.jvm.lower.inlineclasses.isInlineClassFieldGetter
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.createType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.load.java.JavaDescriptorVisibilities
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.types.Variance

internal val propertyReferencePhase = makeIrFilePhase(
    ::PropertyReferenceLowering,
    name = "PropertyReference",
    description = "Construct KProperty instances returned by expressions such as A::x and A()::x",
    // This must be done after contents of functions are extracted into separate classes, or else the `$$delegatedProperties`
    // field will end up in the wrong class (not the one that declares the delegated property).
    prerequisite = setOf(functionReferencePhase, suspendLambdaPhase)
)

private class PropertyReferenceLowering(val context: JvmBackendContext) : IrElementTransformerVoidWithContext(), FileLoweringPass {
    // TODO: join IrLocalDelegatedPropertyReference and IrPropertyReference via the class hierarchy?
    private val IrMemberAccessExpression<*>.getter: IrSimpleFunctionSymbol?
        get() = (this as? IrPropertyReference)?.getter ?: (this as? IrLocalDelegatedPropertyReference)?.getter

    private val IrMemberAccessExpression<*>.setter: IrSimpleFunctionSymbol?
        get() = (this as? IrPropertyReference)?.setter ?: (this as? IrLocalDelegatedPropertyReference)?.setter

    private val IrMemberAccessExpression<*>.field: IrFieldSymbol?
        get() = (this as? IrPropertyReference)?.field

    private val arrayItemGetter =
        context.ir.symbols.array.owner.functions.single { it.name.asString() == "get" }

    private val signatureStringIntrinsic = context.ir.symbols.signatureStringIntrinsic

    private val kPropertyStarType = IrSimpleTypeImpl(
        context.irBuiltIns.kPropertyClass,
        false,
        listOf(makeTypeProjection(context.irBuiltIns.anyNType, Variance.OUT_VARIANCE)),
        emptyList()
    )

    private val kPropertiesFieldType =
        context.ir.symbols.array.createType(false, listOf(makeTypeProjection(kPropertyStarType, Variance.OUT_VARIANCE)))

    private val useOptimizedSuperClass =
        context.state.generateOptimizedCallableReferenceSuperClasses

    private val IrMemberAccessExpression<*>.propertyContainer: IrDeclarationParent
        get() = if (this is IrLocalDelegatedPropertyReference)
            currentClassData?.localPropertyOwner(getter)
                ?: throw AssertionError("local property reference before declaration: ${render()}")
        else
            getter?.owner?.parent ?: field?.owner?.parent ?: error("Property without getter or field: ${dump()}")

    // Plain Java fields do not have a getter, but can be referenced nonetheless. The signature should be the one
    // that a getter would have, if it existed.
    private val IrField.fakeGetterSignature: String
        get() = "${JvmAbi.getterName(name.asString())}()${context.methodSignatureMapper.mapReturnType(this)}"

    private fun IrBuilderWithScope.computeSignatureString(expression: IrMemberAccessExpression<*>): IrExpression {
        if (expression is IrLocalDelegatedPropertyReference) {
            // Local delegated properties are stored as a plain list, and the runtime library extracts the index from this string:
            val index = currentClassData?.localPropertyIndex(expression.getter)
                ?: throw AssertionError("local property reference before declaration: ${expression.render()}")
            return irString("<v#$index>")
        }
        val getter = expression.getter ?: return irString(expression.field!!.owner.fakeGetterSignature)
        // Work around for differences between `RuntimeTypeMapper.KotlinProperty` and the real Kotlin type mapper.
        // Most notably, the runtime type mapper does not perform inline class name mangling. This is usually not
        // a problem, since we will produce a getter signature as part of the Kotlin metadata, except when there
        // is no getter method in the bytecode. In that case we need to avoid inline class mangling for the
        // function reference used in the <signature-string> intrinsic.
        //
        // Note that we cannot compute the signature at this point, since we still need to mangle the names of
        // private properties in multifile-part classes.
        val needsDummySignature = getter.owner.correspondingPropertySymbol?.owner?.needsAccessor(getter.owner) == false ||
                // Internal underlying vals of inline classes have no getter method
                getter.owner.isInlineClassFieldGetter && getter.owner.visibility == DescriptorVisibilities.INTERNAL
        val origin = if (needsDummySignature) InlineClassAbi.UNMANGLED_FUNCTION_REFERENCE else null
        val reference =
            IrFunctionReferenceImpl.fromSymbolOwner(UNDEFINED_OFFSET, UNDEFINED_OFFSET, expression.type, getter, 0, getter, origin)
        return irCall(signatureStringIntrinsic).apply { putValueArgument(0, reference) }
    }

    private fun IrClass.addOverride(method: IrSimpleFunction, buildBody: IrBuilderWithScope.(List<IrValueParameter>) -> IrExpression) =
        addFunction {
            setSourceRange(this@addOverride)
            name = method.name
            returnType = method.returnType
            visibility = method.visibility
            modality = Modality.OPEN
            origin = JvmLoweredDeclarationOrigin.GENERATED_MEMBER_IN_CALLABLE_REFERENCE
        }.apply {
            overriddenSymbols += method.symbol
            dispatchReceiverParameter = thisReceiver!!.copyTo(this)
            valueParameters = method.valueParameters.map { it.copyTo(this) }
            body = context.createIrBuilder(symbol, startOffset, endOffset).run {
                irExprBody(buildBody(listOf(dispatchReceiverParameter!!) + valueParameters))
            }
        }

    private fun IrClass.addFakeOverride(method: IrSimpleFunction) =
        addFunction {
            name = method.name
            returnType = method.returnType
            visibility = method.visibility
            isFakeOverride = true
            origin = IrDeclarationOrigin.FAKE_OVERRIDE
        }.apply {
            overriddenSymbols += method.symbol
            dispatchReceiverParameter = thisReceiver!!.copyTo(this)
            valueParameters = method.valueParameters.map { it.copyTo(this) }
        }

    private class PropertyReferenceKind(
        val interfaceSymbol: IrClassSymbol,
        val implSymbol: IrClassSymbol,
        val wrapper: IrFunction
    )

    private fun propertyReferenceKind(mutable: Boolean, i: Int) = PropertyReferenceKind(
        context.ir.symbols.getPropertyReferenceClass(mutable, i, false),
        context.ir.symbols.getPropertyReferenceClass(mutable, i, true),
        context.ir.symbols.reflection.owner.functions.single { it.name.asString() == (if (mutable) "mutableProperty$i" else "property$i") }
    )

    private fun propertyReferenceKindFor(expression: IrMemberAccessExpression<*>): PropertyReferenceKind =
        expression.getter?.owner?.let {
            val boundReceivers = listOfNotNull(expression.dispatchReceiver, expression.extensionReceiver).size
            val needReceivers = listOfNotNull(it.dispatchReceiverParameter, it.extensionReceiverParameter).size
            // PropertyReference1 will swap the receivers if bound with the extension one, and PropertyReference0
            // has no way to bind two receivers at once.
            if (boundReceivers == 2 || (expression.extensionReceiver != null && needReceivers == 2))
                TODO("property reference with 2 receivers")
            propertyReferenceKind(expression.setter != null, needReceivers - boundReceivers)
        } ?: expression.field?.owner?.let {
            propertyReferenceKind(!it.isFinal, if (it.isStatic || expression.dispatchReceiver != null) 0 else 1)
        } ?: throw AssertionError("property has no getter and no field: ${expression.dump()}")

    private data class PropertyInstance(val initializer: IrExpression, val index: Int)

    private inner class ClassData(val irClass: IrClass, val parent: ClassData?) {
        val kProperties = mutableMapOf<IrSymbol, PropertyInstance>()
        val kPropertiesField = context.irFactory.buildField {
            name = Name.identifier(JvmAbi.DELEGATED_PROPERTIES_ARRAY_NAME)
            type = kPropertiesFieldType
            origin = JvmLoweredDeclarationOrigin.GENERATED_PROPERTY_REFERENCE
            isFinal = true
            isStatic = true
            visibility = JavaDescriptorVisibilities.PACKAGE_VISIBILITY
        }

        val localProperties = mutableListOf<IrLocalDelegatedPropertySymbol>()
        val localPropertyIndices = mutableMapOf<IrSymbol, Int>()
        val isSynthetic = irClass.metadata !is MetadataSource.File && irClass.metadata !is MetadataSource.Class

        fun localPropertyIndex(getter: IrSymbol): Int? =
            localPropertyIndices[getter] ?: parent?.localPropertyIndex(getter)

        fun localPropertyOwner(getter: IrSymbol): IrClass? =
            if (getter in localPropertyIndices) irClass else parent?.localPropertyOwner(getter)

        fun rememberLocalProperty(property: IrLocalDelegatedProperty) {
            // Prefer to attach metadata to non-synthetic classes, because it won't be serialized otherwise;
            // if not possible, though, putting it right here will at least allow non-reflective uses.
            val metadataOwner = generateSequence(this) { it.parent }.find { !it.isSynthetic } ?: this
            metadataOwner.localPropertyIndices[property.getter.symbol] = metadataOwner.localProperties.size
            metadataOwner.localProperties.add(property.symbol)
        }
    }

    private var currentClassData: ClassData? = null

    override fun lower(irFile: IrFile) =
        irFile.transformChildrenVoid()

    override fun visitClassNew(declaration: IrClass): IrStatement {
        val data = ClassData(declaration, currentClassData)
        currentClassData = data
        declaration.transformChildrenVoid()
        currentClassData = data.parent

        // Put the new field at the beginning so that static delegated properties with initializers work correctly.
        // Since we do not cache property references with bound receivers, the new field does not reference anything else.
        if (data.kProperties.isNotEmpty()) {
            declaration.declarations.add(0, data.kPropertiesField.apply {
                parent = declaration
                initializer = context.createJvmIrBuilder(declaration.symbol).run {
                    val initializers = data.kProperties.values.sortedBy { it.index }.map { it.initializer }
                    irExprBody(irArrayOf(kPropertiesFieldType, initializers))
                }
            })
        }
        if (data.localProperties.isNotEmpty()) {
            context.localDelegatedProperties[declaration.attributeOwnerId] = data.localProperties
        }
        return declaration
    }

    override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty): IrStatement {
        currentClassData!!.rememberLocalProperty(declaration)
        return super.visitLocalDelegatedProperty(declaration)
    }

    override fun visitPropertyReference(expression: IrPropertyReference): IrExpression =
        cachedKProperty(expression)

    override fun visitLocalDelegatedPropertyReference(expression: IrLocalDelegatedPropertyReference): IrExpression =
        cachedKProperty(expression)

    private fun cachedKProperty(expression: IrCallableReference<*>): IrExpression {
        expression.transformChildrenVoid()
        if (expression.origin != IrStatementOrigin.PROPERTY_REFERENCE_FOR_DELEGATE)
            return createSpecializedKProperty(expression)

        val data = currentClassData ?: throw AssertionError("property reference not in class: ${expression.render()}")
        // For delegated properties, the getter and setter contain a reference each as the second argument to getValue
        // and setValue. Since it's highly unlikely that anyone will call get/set on these, optimize for space.
        return context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol, expression.startOffset, expression.endOffset).run {
            val (_, index) = data.kProperties.getOrPut(expression.symbol) {
                PropertyInstance(createReflectedKProperty(expression), data.kProperties.size)
            }
            irCall(arrayItemGetter).apply {
                dispatchReceiver = irGetField(null, data.kPropertiesField)
                putValueArgument(0, irInt(index))
            }
        }
    }

    // Create an instance of KProperty that uses Java reflection to locate the getter and the setter. This kind of reference
    // does not support local variables or bound receivers (e.g. `Class()::field`) and is slower, but takes up less space.
    // Example: `C::property` -> `Reflection.property1(PropertyReference1Impl(C::class, "property", "getProperty()LType;"))`.
    private fun createReflectedKProperty(expression: IrCallableReference<*>): IrExpression {
        assert(expression.dispatchReceiver == null && expression.extensionReceiver == null) {
            "cannot create a reflected KProperty if the reference has a bound receiver: ${expression.render()}"
        }
        val referenceKind = propertyReferenceKindFor(expression)
        return context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol, expression.startOffset, expression.endOffset).run {
            irCall(referenceKind.wrapper).apply {
                val constructor = referenceKind.implSymbol.constructors.single { it.owner.valueParameters.size == 3 }
                putValueArgument(0, irCall(constructor).apply {
                    putValueArgument(0, calculateOwner(expression.propertyContainer, this@PropertyReferenceLowering.context))
                    putValueArgument(1, irString(expression.referencedName.asString()))
                    putValueArgument(2, computeSignatureString(expression))
                })
            }
        }
    }

    // Create an instance of KProperty that overrides the get() and set() methods to directly call getX() and setX() on the object.
    // This is (relatively) fast, but space-inefficient. Also, the instances can store bound receivers in their fields. Example:
    //
    //    class C$property$0 : PropertyReference0Impl {
    //        constructor(boundReceiver: C) : super(boundReceiver, C::class.java, "property", "getProperty()LType;", 0)
    //        override fun get(): T = receiver.property
    //        override fun set(value: T) { receiver.property = value }
    //    }
    //
    // and then `C()::property` -> `C$property$0(C())`.
    //
    private fun createSpecializedKProperty(expression: IrCallableReference<*>): IrExpression {
        // We do not reuse classes for non-reflective property references because they would not have
        // a valid enclosing method if the same property is referenced at many points.
        val referenceClass = createKPropertySubclass(expression)
        return context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol, expression.startOffset, expression.endOffset).irBlock {
            +referenceClass
            +irCall(referenceClass.constructors.single()).apply {
                var index = 0
                expression.dispatchReceiver?.let { putValueArgument(index++, it) }
                expression.extensionReceiver?.let { putValueArgument(index++, it) }
            }
        }
    }

    private fun createKPropertySubclass(expression: IrCallableReference<*>): IrClass {
        val kind = propertyReferenceKindFor(expression)
        val superClass = if (useOptimizedSuperClass) kind.implSymbol.owner else kind.interfaceSymbol.owner
        val referenceClass = context.irFactory.buildClass {
            setSourceRange(expression)
            name = SpecialNames.NO_NAME_PROVIDED
            origin = JvmLoweredDeclarationOrigin.GENERATED_PROPERTY_REFERENCE
            visibility = DescriptorVisibilities.LOCAL
        }.apply {
            parent = currentDeclarationParent!!
            superTypes = listOf(superClass.defaultType)
            createImplicitParameterDeclarationWithWrappedDescriptor()
        }.copyAttributes(expression)

        addConstructor(expression, referenceClass, superClass)

        if (!useOptimizedSuperClass) {
            val getName = superClass.functions.single { it.name.asString() == "getName" }
            val getOwner = superClass.functions.single { it.name.asString() == "getOwner" }
            val getSignature = superClass.functions.single { it.name.asString() == "getSignature" }
            referenceClass.addOverride(getName) { irString(expression.referencedName.asString()) }
            referenceClass.addOverride(getOwner) { calculateOwner(expression.propertyContainer, this@PropertyReferenceLowering.context) }
            referenceClass.addOverride(getSignature) { computeSignatureString(expression) }
        }

        val backingField = superClass.properties.single { it.name.asString() == "receiver" }.backingField!!
        val get = superClass.functions.find { it.name.asString() == "get" }
        val set = superClass.functions.find { it.name.asString() == "set" }
        val invoke = superClass.functions.find { it.name.asString() == "invoke" }

        val field = expression.field?.owner
        if (field == null) {
            fun IrBuilderWithScope.setCallArguments(call: IrCall, arguments: List<IrValueParameter>) {
                var index = 1
                call.copyTypeArgumentsFrom(expression)
                call.dispatchReceiver = call.symbol.owner.dispatchReceiverParameter?.let {
                    if (expression.dispatchReceiver != null)
                        irImplicitCast(irGetField(irGet(arguments[0]), backingField), it.type)
                    else
                        irImplicitCast(irGet(arguments[index++]), it.type)
                }
                call.extensionReceiver = call.symbol.owner.extensionReceiverParameter?.let {
                    if (expression.extensionReceiver != null)
                        irImplicitCast(irGetField(irGet(arguments[0]), backingField), it.type)
                    else
                        irImplicitCast(irGet(arguments[index++]), it.type)
                }
            }

            expression.getter?.owner?.let { getter ->
                referenceClass.addOverride(get!!) { arguments ->
                    irGet(getter.returnType, null, getter.symbol).apply {
                        setCallArguments(this, arguments)
                    }
                }
                referenceClass.addFakeOverride(invoke!!)
            }

            expression.setter?.owner?.let { setter ->
                referenceClass.addOverride(set!!) { arguments ->
                    irSet(setter.returnType, null, setter.symbol, irGet(arguments.last())).apply {
                        setCallArguments(this, arguments)
                    }
                }
            }
        } else {
            fun IrBuilderWithScope.fieldReceiver(arguments: List<IrValueParameter>) = when {
                field.isStatic ->
                    null
                expression.dispatchReceiver != null ->
                    irImplicitCast(irGetField(irGet(arguments[0]), backingField), field.parentAsClass.defaultType)
                else ->
                    irImplicitCast(irGet(arguments[1]), field.parentAsClass.defaultType)
            }

            referenceClass.addOverride(get!!) { arguments ->
                irGetField(fieldReceiver(arguments), field)
            }

            if (!field.isFinal) {
                referenceClass.addOverride(set!!) { arguments ->
                    irSetField(fieldReceiver(arguments), field, irGet(arguments.last()))
                }
            }
        }
        return referenceClass
    }

    private fun addConstructor(expression: IrCallableReference<*>, referenceClass: IrClass, superClass: IrClass) {
        // See propertyReferenceKindFor -- only one of them could ever be present.
        val hasBoundReceiver = expression.dispatchReceiver != null || expression.extensionReceiver != null
        val numOfSuperArgs =
            (if (hasBoundReceiver) 1 else 0) + (if (useOptimizedSuperClass) 4 else 0)
        val superConstructor = superClass.constructors.single { it.valueParameters.size == numOfSuperArgs }

        if (!useOptimizedSuperClass) {
            referenceClass.addSimpleDelegatingConstructor(superConstructor, context.irBuiltIns, isPrimary = true)
            return
        }

        referenceClass.addConstructor {
            origin = JvmLoweredDeclarationOrigin.GENERATED_MEMBER_IN_CALLABLE_REFERENCE
            isPrimary = true
        }.apply {
            if (hasBoundReceiver) {
                addValueParameter("receiver", context.irBuiltIns.anyNType)
            }
            body = context.createJvmIrBuilder(symbol).run {
                irBlockBody(startOffset, endOffset) {
                    +irDelegatingConstructorCall(superConstructor).apply {
                        var index = 0
                        if (hasBoundReceiver) {
                            putValueArgument(index++, irGet(valueParameters.first()))
                        }
                        val callee = expression.symbol.owner as IrDeclaration
                        val owner = calculateOwnerKClass(expression.propertyContainer, backendContext)
                        putValueArgument(index++, kClassToJavaClass(owner, backendContext))
                        putValueArgument(index++, irString(expression.referencedName.asString()))
                        putValueArgument(index++, computeSignatureString(expression))
                        putValueArgument(index, irInt(FunctionReferenceLowering.getCallableReferenceTopLevelFlag(callee)))
                    }
                    +IrInstanceInitializerCallImpl(startOffset, endOffset, referenceClass.symbol, context.irBuiltIns.unitType)
                }
            }
        }
    }
}
