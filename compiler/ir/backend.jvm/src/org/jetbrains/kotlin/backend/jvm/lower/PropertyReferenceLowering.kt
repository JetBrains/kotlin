/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
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
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.createType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.java.JavaVisibilities
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.types.Variance

internal val propertyReferencePhase = makeIrFilePhase(
    ::PropertyReferenceLowering,
    name = "PropertyReference",
    description = "Construct KProperty instances returned by expressions such as A::x and A()::x"
)

internal class PropertyReferenceLowering(val context: JvmBackendContext) : ClassLoweringPass {
    // Reflection metadata for local properties is serialized under the signature "<v#$N>" attached to the containing class.
    // This maps properties to values of N.
    private val localPropertyIndices = mutableMapOf<IrSymbol, Int>()

    // TODO: join IrLocalDelegatedPropertyReference and IrPropertyReference via the class hierarchy?
    private val IrMemberAccessExpression.getter: IrSimpleFunctionSymbol?
        get() = (this as? IrPropertyReference)?.getter ?: (this as? IrLocalDelegatedPropertyReference)?.getter

    private val IrMemberAccessExpression.setter: IrSimpleFunctionSymbol?
        get() = (this as? IrPropertyReference)?.setter ?: (this as? IrLocalDelegatedPropertyReference)?.setter

    private val IrMemberAccessExpression.field: IrFieldSymbol?
        get() = (this as? IrPropertyReference)?.field

    private val IrSimpleFunction.signature: String
        get() = context.methodSignatureMapper.mapSignatureSkipGeneric(collectRealOverrides().first()).toString()

    // Plain Java fields do not have a getter, but can be referenced nonetheless. The signature should be the one
    // that a getter would have, if it existed.
    private val IrField.signature: String
        get() = "${JvmAbi.getterName(name.asString())}()${context.methodSignatureMapper.mapReturnType(this)}"

    private val IrMemberAccessExpression.signature: String
        get() = getter?.let { getter ->
            localPropertyIndices[getter]?.let { "<v#$it>" }
        } ?: getter?.owner?.signature ?: field!!.owner.signature

    private val arrayItemGetter =
        context.ir.symbols.array.owner.functions.single { it.name.asString() == "get" }

    private val kPropertyStarType = IrSimpleTypeImpl(
        context.irBuiltIns.kPropertyClass,
        false,
        listOf(makeTypeProjection(context.irBuiltIns.anyNType, Variance.OUT_VARIANCE)),
        emptyList()
    )

    private val kPropertiesFieldType =
        context.ir.symbols.array.createType(false, listOf(makeTypeProjection(kPropertyStarType, Variance.OUT_VARIANCE)))

    private val IrMemberAccessExpression.propertyContainer: IrDeclarationParent
        get() {
            var current: IrDeclaration = getter?.owner ?: field?.owner ?: error("Property without getter or field: ${dump()}")
            while (current.parent is IrFunction)
                current = current.parent as IrFunction // Local delegated property.
            return current.parent
        }

    private fun IrBuilderWithScope.buildReflectedContainerReference(expression: IrMemberAccessExpression): IrExpression =
        with(CallableReferenceLowering) {
            calculateOwner(expression.propertyContainer, this@PropertyReferenceLowering.context)
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
            overriddenSymbols.add(method.symbol)
            dispatchReceiverParameter = thisReceiver!!.copyTo(this)
            for (parameter in method.valueParameters)
                valueParameters.add(parameter.copyTo(this))
            body = context.createIrBuilder(symbol, startOffset, endOffset).run {
                irExprBody(buildBody(listOf(dispatchReceiverParameter!!) + valueParameters))
            }
        }

    private fun IrClass.addFakeOverride(method: IrSimpleFunction) =
        addFunction {
            name = method.name
            returnType = method.returnType
            visibility = method.visibility
            origin = IrDeclarationOrigin.FAKE_OVERRIDE
        }.apply {
            overriddenSymbols.add(method.symbol)
            dispatchReceiverParameter = thisReceiver!!.copyTo(this)
            for (parameter in method.valueParameters)
                valueParameters.add(parameter.copyTo(this))
        }

    private class PropertyReferenceKind(
        val interfaceSymbol: IrClassSymbol,
        val reflectedSymbol: IrClassSymbol,
        val wrapper: IrFunction
    )

    private fun propertyReferenceKind(mutable: Boolean, i: Int) = PropertyReferenceKind(
        context.ir.symbols.getPropertyReferenceClass(mutable, i, false),
        context.ir.symbols.getPropertyReferenceClass(mutable, i, true),
        context.ir.symbols.reflection.owner.functions.single { it.name.asString() == (if (mutable) "mutableProperty$i" else "property$i") }
    )

    private fun propertyReferenceKindFor(expression: IrMemberAccessExpression): PropertyReferenceKind =
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

    override fun lower(irClass: IrClass) {
        val kProperties = mutableMapOf<IrSymbol, PropertyInstance>()
        val kPropertiesField = buildField {
            name = Name.identifier(JvmAbi.DELEGATED_PROPERTIES_ARRAY_NAME)
            type = kPropertiesFieldType
            origin = JvmLoweredDeclarationOrigin.GENERATED_PROPERTY_REFERENCE
            isFinal = true
            isStatic = true
            visibility = JavaVisibilities.PACKAGE_VISIBILITY
        }
        var localPropertiesInClass = 0

        irClass.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {
            override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty): IrStatement {
                localPropertyIndices[declaration.getter.symbol] = localPropertiesInClass++
                return super.visitLocalDelegatedProperty(declaration)
            }

            override fun visitPropertyReference(expression: IrPropertyReference): IrExpression =
                cachedKProperty(expression)

            override fun visitLocalDelegatedPropertyReference(expression: IrLocalDelegatedPropertyReference): IrExpression =
                cachedKProperty(expression)

            private fun cachedKProperty(expression: IrCallableReference): IrExpression {
                if (expression.origin != IrStatementOrigin.PROPERTY_REFERENCE_FOR_DELEGATE)
                    return createSpecializedKProperty(expression)

                // For delegated properties, the getter and setter contain a reference each as the second argument to getValue
                // and setValue. Since it's highly unlikely that anyone will call get/set on these, optimize for space.
                return context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol, expression.startOffset, expression.endOffset).run {
                    val (_, index) = kProperties.getOrPut(expression.symbol) {
                        PropertyInstance(createReflectedKProperty(expression), kProperties.size)
                    }
                    irCall(arrayItemGetter).apply {
                        dispatchReceiver = irGetField(null, kPropertiesField)
                        putValueArgument(0, irInt(index))
                    }
                }
            }

            // Create an instance of KProperty that uses Java reflection to locate the getter and the setter. This kind of reference
            // does not support local variables or bound receivers (e.g. `Class()::field`) and is slower, but takes up less space.
            // Example: `C::property` -> `Reflection.property1(PropertyReference1Impl(C::class, "property", "getProperty()LType;"))`.
            private fun createReflectedKProperty(expression: IrCallableReference): IrExpression {
                val referenceKind = propertyReferenceKindFor(expression)
                return context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol, expression.startOffset, expression.endOffset).run {
                    irCall(referenceKind.wrapper).apply {
                        putValueArgument(0, irCall(referenceKind.reflectedSymbol.constructors.single()).apply {
                            putValueArgument(0, buildReflectedContainerReference(expression))
                            putValueArgument(1, irString(expression.symbol.descriptor.name.asString()))
                            putValueArgument(2, irString(expression.signature))
                        })
                    }
                }
            }

            // Create an instance of KProperty that overrides the get() and set() methods to directly call getX() and setX() on the object.
            // This is (relatively) fast, but space-inefficient. Also, the instances can store bound receivers in their fields. Example:
            //
            //    class C$property$0 : PropertyReference0 {
            //        constructor(boundReceiver: C) : super(boundReceiver)
            //        override val name = "property"
            //        override fun getOwner() = C::class
            //        override fun getSignature() = "getProperty()LType;"
            //        override fun get(): T = receiver.property
            //        override fun set(value: T) { receiver.property = value }
            //    }
            //
            // and then `C()::property` -> `C$property$0(C())`.
            //
            private fun createSpecializedKProperty(expression: IrCallableReference): IrExpression {
                val referenceClass = createKPropertySubclass(expression)
                return context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol, expression.startOffset, expression.endOffset)
                    .irBlock {
                        +referenceClass
                        +irCall(referenceClass.constructors.single()).apply {
                            var index = 0
                            expression.dispatchReceiver?.let { putValueArgument(index++, it) }
                            expression.extensionReceiver?.let { putValueArgument(index++, it) }
                        }
                    }
            }

            private fun createKPropertySubclass(expression: IrCallableReference): IrClass {
                val superClass = propertyReferenceKindFor(expression).interfaceSymbol.owner
                val referenceClass = buildClass {
                    setSourceRange(expression)
                    name = SpecialNames.NO_NAME_PROVIDED
                    origin = JvmLoweredDeclarationOrigin.GENERATED_PROPERTY_REFERENCE
                    visibility = Visibilities.LOCAL
                }.apply {
                    parent = irClass
                    superTypes += IrSimpleTypeImpl(superClass.symbol, false, listOf(), listOf())
                    createImplicitParameterDeclarationWithWrappedDescriptor()
                }.copyAttributes(expression)

                // See propertyReferenceKindFor -- only one of them could ever be present.
                val numOfSuperArgs = if (expression.dispatchReceiver != null || expression.extensionReceiver != null) 1 else 0
                val superConstructor = superClass.constructors.single { it.valueParameters.size == numOfSuperArgs }
                val backingFieldFromSuper = superClass.properties.single { it.name.asString() == "receiver" }.backingField!!
                val getName = superClass.functions.single { it.name.asString() == "getName" }
                val getOwner = superClass.functions.single { it.name.asString() == "getOwner" }
                val getSignature = superClass.functions.single { it.name.asString() == "getSignature" }
                val get = superClass.functions.find { it.name.asString() == "get" }
                val set = superClass.functions.find { it.name.asString() == "set" }
                val invoke = superClass.functions.find { it.name.asString() == "invoke" }

                referenceClass.addSimpleDelegatingConstructor(superConstructor, context.irBuiltIns, isPrimary = true)
                referenceClass.addOverride(getName) { irString(expression.symbol.descriptor.name.asString()) }
                referenceClass.addOverride(getOwner) { buildReflectedContainerReference(expression) }
                referenceClass.addOverride(getSignature) { irString(expression.signature) }

                val receiverField = referenceClass.addField {
                    name = backingFieldFromSuper.name
                    origin = IrDeclarationOrigin.FAKE_OVERRIDE
                    type = backingFieldFromSuper.type
                    isFinal = backingFieldFromSuper.isFinal
                    isStatic = backingFieldFromSuper.isStatic
                    visibility = backingFieldFromSuper.visibility
                }

                val field = expression.field?.owner
                if (field == null) {
                    fun IrBuilderWithScope.setCallArguments(call: IrCall, arguments: List<IrValueParameter>) {
                        var index = 1
                        call.copyTypeArgumentsFrom(expression)
                        call.dispatchReceiver = call.symbol.owner.dispatchReceiverParameter?.let {
                            if (expression.dispatchReceiver != null)
                                irImplicitCast(irGetField(irGet(arguments[0]), receiverField), it.type)
                            else
                                irImplicitCast(irGet(arguments[index++]), it.type)
                        }
                        call.extensionReceiver = call.symbol.owner.extensionReceiverParameter?.let {
                            if (expression.extensionReceiver != null)
                                irImplicitCast(irGetField(irGet(arguments[0]), receiverField), it.type)
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
                            irImplicitCast(irGetField(irGet(arguments[0]), receiverField), field.parentAsClass.defaultType)
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
        })

        // Put the new field at the beginning so that static delegated properties with initializers work correctly.
        // Since we do not cache property references, the new field does not reference anything else.
        if (kProperties.isNotEmpty()) {
            irClass.declarations.add(0, kPropertiesField.apply {
                parent = irClass
                initializer = context.createJvmIrBuilder(irClass.symbol).run {
                    val initializers = kProperties.values.sortedBy { it.index }.map { it.initializer }
                    irExprBody(irArrayOf(kPropertiesFieldType, initializers))
                }
            })

            context.localDelegatedProperties[irClass.attributeOwnerId as IrClass] =
                kProperties.keys.filterIsInstance<IrLocalDelegatedPropertySymbol>()
        }
    }
}
