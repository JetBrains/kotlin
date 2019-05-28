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
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.createType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name
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
        get() = context.state.typeMapper.mapSignatureSkipGeneric(collectRealOverrides().first().descriptor).toString()

    // Plain Java fields do not have a getter, but can be referenced nonetheless. The signature should be the one
    // that a getter would have, if it existed.
    private val IrField.signature: String
        get() = "${JvmAbi.getterName(name.asString())}()${context.state.typeMapper.mapReturnType(descriptor)}"

    private val IrMemberAccessExpression.signature: String
        get() = getter?.let { getter -> localPropertyIndices[getter]?.let { "<v#$it>" } } ?: getter?.owner?.signature ?: field!!.owner.signature

    private val IrMemberAccessExpression.symbol: IrSymbol
        get() = getter?.owner?.symbol ?: field!!.owner.symbol

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

    // Return some declaration the parent of which is the containing class/package for the referenced property. This is
    // a bit of a hack, as the reason for not returning the container itself is the `mapImplementationOwner` call below.
    // TODO: move after LocalDeclarationsLowering and always use the getter/field? Can't move below ConstLowering
    //       right now -- get() would not be optimized.
    private val IrMemberAccessExpression.propertyContainerChild: IrDeclaration?
        get() {
            var current: IrDeclaration? = getter?.owner ?: field?.owner
            while (current?.parent is IrFunction)
                current = current.parent as IrFunction // Local delegated property.
            return current
        }

    // TODO: remove code duplication with CallableReferenceLowering
    private val IrMemberAccessExpression.parentJavaClassReference
        get() = IrClassReferenceImpl(
            startOffset, endOffset,
            context.ir.symbols.javaLangClass.typeWith(),
            context.ir.symbols.javaLangClass,
            // TODO: when the parent is an interface, this should map to DefaultImpls. However, that requires
            //       moving this lowering below InterfaceLowering; see comment about the ordering above, though.
            CrIrType(context.state.typeMapper.mapImplementationOwner(propertyContainerChild!!.descriptor))
        )

    private fun IrBuilderWithScope.buildReflectedContainerReference(expression: IrMemberAccessExpression): IrExpression {
        val parent = expression.propertyContainerChild?.parent
        val context = this@PropertyReferenceLowering.context
        return when {
            // FileClassLowering creates a class to which all package-level declarations are moved. However, there
            // can still be external declarations at the package level, which is why we check for both a file class
            // and a package fragment.
            parent is IrPackageFragment || (parent is IrClass && parent.origin == IrDeclarationOrigin.FILE_CLASS) ->
                irCall(context.ir.symbols.getOrCreateKotlinPackage).apply {
                    putValueArgument(0, expression.parentJavaClassReference)
                    putValueArgument(1, irString(context.state.moduleName))
                }
            parent is IrClass ->
                irCall(context.ir.symbols.getOrCreateKotlinClass).apply {
                    putValueArgument(0, expression.parentJavaClassReference)
                }
            else -> throw AssertionError("referenced property not inside a class/package fragment")
        }
    }

    private fun IrClass.addOverride(method: IrSimpleFunction, buildBody: IrBuilderWithScope.(List<IrValueParameter>) -> IrExpression) =
        addFunction {
            setSourceRange(this@addOverride)
            name = method.name
            returnType = method.returnType
            visibility = method.visibility
            origin = this@addOverride.origin
        }.apply {
            overriddenSymbols.add(method.symbol)
            dispatchReceiverParameter = thisReceiver!!.copyTo(this)
            for (parameter in method.valueParameters)
                valueParameters.add(parameter.copyTo(this))
            body = context.createIrBuilder(symbol, startOffset, endOffset).run {
                irExprBody(buildBody(listOf(dispatchReceiverParameter!!) + valueParameters))
            }
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

    private data class PropertyCacheKey(val symbol: IrSymbol, val reflected: Boolean)
    private data class PropertyClassCacheKey(val symbol: IrSymbol, val boundReceiver: Boolean)
    private data class PropertyInstance(val initializer: IrExpression, val index: Int)

    override fun lower(irClass: IrClass) {
        val kProperties = mutableMapOf<PropertyCacheKey, PropertyInstance>()
        val kPropertyClasses = mutableMapOf<PropertyClassCacheKey, IrClass>()
        val kPropertiesField = buildField {
            name = Name.identifier(JvmAbi.DELEGATED_PROPERTIES_ARRAY_NAME)
            type = kPropertiesFieldType
            origin = JvmLoweredDeclarationOrigin.GENERATED_PROPERTY_REFERENCE
            isFinal = true
            isStatic = true
            // TODO: make the visibility package-local. Currently it's more permissive to allow access from inline functions.
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

            private fun cachedKProperty(expression: IrMemberAccessExpression): IrExpression {
                // Reflected implementation does not support partial application; also, cannot cache instances with arguments.
                if (expression.dispatchReceiver != null || expression.extensionReceiver != null)
                    return createSpecializedKProperty(expression)

                // For delegated properties, the getter and setter contain a reference each as the second argument to getValue
                // and setValue. Since it's highly unlikely that anyone will call get/set on these, optimize for space.
                val useReflectedImpl = expression.origin == IrStatementOrigin.PROPERTY_REFERENCE_FOR_DELEGATE
                return context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol, expression.startOffset, expression.endOffset).run {
                    val (_, index) = kProperties.getOrPut(PropertyCacheKey(expression.symbol, useReflectedImpl)) {
                        val kProperty = if (useReflectedImpl)
                            createReflectedKProperty(expression)
                        else
                            createSpecializedKProperty(expression)
                        PropertyInstance(kProperty, kProperties.size)
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
            private fun createReflectedKProperty(expression: IrMemberAccessExpression): IrExpression {
                val referenceKind = propertyReferenceKindFor(expression)
                return context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol, expression.startOffset, expression.endOffset).run {
                    irCall(referenceKind.wrapper).apply {
                        putValueArgument(0, irCall(referenceKind.reflectedSymbol.constructors.single()).apply {
                            putValueArgument(0, buildReflectedContainerReference(expression))
                            putValueArgument(1, irString(expression.descriptor.name.asString()))
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
            private fun createSpecializedKProperty(expression: IrMemberAccessExpression): IrExpression {
                val bound = expression.dispatchReceiver != null || expression.extensionReceiver != null
                val referenceClass = kPropertyClasses.getOrPut(PropertyClassCacheKey(expression.symbol, bound)) {
                    createKPropertySubclass(expression)
                }
                return context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol, expression.startOffset, expression.endOffset).run {
                    irCall(referenceClass.constructors.single()).apply {
                        var index = 0
                        expression.dispatchReceiver?.let { putValueArgument(index++, it) }
                        expression.extensionReceiver?.let { putValueArgument(index++, it) }
                    }
                }
            }

            private fun createKPropertySubclass(expression: IrMemberAccessExpression): IrClass {
                val superClass = propertyReferenceKindFor(expression).interfaceSymbol.owner
                val referenceClass = buildClass {
                    setSourceRange(expression)
                    name = Name.identifier("${expression.descriptor.name}\$${kPropertyClasses.size}")
                    origin = JvmLoweredDeclarationOrigin.GENERATED_PROPERTY_REFERENCE
                }.apply {
                    parent = irClass
                    superTypes += IrSimpleTypeImpl(superClass.symbol, false, listOf(), listOf())
                    createImplicitParameterDeclarationWithWrappedDescriptor()
                }

                // See propertyReferenceKindFor -- only one of them could ever be present.
                val numOfSuperArgs = if (expression.dispatchReceiver != null || expression.extensionReceiver != null) 1 else 0
                val superConstructor = superClass.constructors.single { it.valueParameters.size == numOfSuperArgs }
                val receiverField = superClass.properties.single { it.name.asString() == "receiver" }.backingField!!
                val getName = superClass.functions.single { it.name.asString() == "getName" }
                val getOwner = superClass.functions.single { it.name.asString() == "getOwner" }
                val getSignature = superClass.functions.single { it.name.asString() == "getSignature" }
                val get = superClass.functions.find { it.name.asString() == "get" }
                val set = superClass.functions.find { it.name.asString() == "set" }

                referenceClass.addSimpleDelegatingConstructor(superConstructor, context.irBuiltIns, isPrimary = true)
                referenceClass.addOverride(getName) { irString(expression.descriptor.name.asString()) }
                referenceClass.addOverride(getOwner) { buildReflectedContainerReference(expression) }
                referenceClass.addOverride(getSignature) { irString(expression.signature) }

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
        // Since we do not cache property references with bound receivers, the new field does not reference anything else.
        if (kProperties.isNotEmpty()) {
            irClass.declarations.add(0, kPropertiesField.apply {
                parent = irClass
                initializer = context.createIrBuilder(irClass.symbol).run {
                    val initializers = kProperties.values.sortedBy { it.index }.map { it.initializer }
                    irExprBody(irCall(this@PropertyReferenceLowering.context.ir.symbols.arrayOf).apply {
                        putValueArgument(0, IrVarargImpl(startOffset, endOffset, kPropertiesFieldType, kPropertyStarType, initializers))
                    })
                }
            })
        }
        irClass.declarations.addAll(0, kPropertyClasses.values)
    }
}
