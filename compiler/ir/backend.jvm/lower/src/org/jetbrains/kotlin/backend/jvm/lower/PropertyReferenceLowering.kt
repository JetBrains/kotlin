/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.backend.jvm.*
import org.jetbrains.kotlin.backend.jvm.ir.*
import org.jetbrains.kotlin.backend.jvm.lower.FunctionReferenceLowering.Companion.calculateOwnerKClass
import org.jetbrains.kotlin.codegen.inline.loadCompiledInlineFunction
import org.jetbrains.kotlin.codegen.optimization.nullCheck.usesLocalExceptParameterNullCheck
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.irFlag
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.createType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.load.java.JavaDescriptorVisibilities
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addToStdlib.assignFrom
import java.util.concurrent.ConcurrentHashMap

/**
 * Constructs `KProperty` instances returned by expressions such as `A::x` and `A()::x`.
 */
@PhaseDescription(
    name = "PropertyReference",
    // This must be done after contents of functions are extracted into separate classes, or else the `$$delegatedProperties`
    // field will end up in the wrong class (not the one that declares the delegated property).
    prerequisite = [FunctionReferenceLowering::class, SuspendLambdaLowering::class, PropertyReferenceDelegationLowering::class],
)
internal class PropertyReferenceLowering(val context: JvmBackendContext) : IrElementTransformerVoidWithContext(), FileLoweringPass {
    companion object {
        // Marking a property reference with this origin causes it to not generate a class.
        val REFLECTED_PROPERTY_REFERENCE by IrStatementOriginImpl
    }

    // TODO: join IrLocalDelegatedPropertyReference and IrPropertyReference via the class hierarchy?
    private val IrMemberAccessExpression<*>.getter: IrSimpleFunctionSymbol?
        get() = (this as? IrPropertyReference)?.getter ?: (this as? IrLocalDelegatedPropertyReference)?.getter

    private val IrMemberAccessExpression<*>.setter: IrSimpleFunctionSymbol?
        get() = (this as? IrPropertyReference)?.setter ?: (this as? IrLocalDelegatedPropertyReference)?.setter

    private val IrMemberAccessExpression<*>.field: IrFieldSymbol?
        get() = (this as? IrPropertyReference)?.field

    private val arrayItemGetter =
        context.symbols.array.owner.functions.single { it.name.asString() == "get" }

    private val signatureStringIntrinsic = context.symbols.signatureStringIntrinsic

    private val kPropertyStarType = IrSimpleTypeImpl(
        context.irBuiltIns.kPropertyClass,
        false,
        listOf(makeTypeProjection(context.irBuiltIns.anyNType, Variance.OUT_VARIANCE)),
        emptyList()
    )

    private val kPropertiesFieldType =
        context.symbols.array.createType(false, listOf(makeTypeProjection(kPropertyStarType, Variance.OUT_VARIANCE)))

    private val IrClass.isSynthetic
        get() = metadata !is MetadataSource.File && metadata !is MetadataSource.Class && metadata !is MetadataSource.Script

    private val IrMemberAccessExpression<*>.propertyContainer: IrDeclarationParent
        get() = if (this is IrLocalDelegatedPropertyReference)
            findClassOwner()
        else
            getter?.owner?.parent ?: field?.owner?.parent ?: error("Property without getter or field: ${dump()}")

    // Plain Java fields do not have a getter, but can be referenced nonetheless. The signature should be the one
    // that a getter would have, if it existed.
    private val IrField.fakeGetterSignature: String
        get() = "${JvmAbi.getterName(name.asString())}()${context.defaultMethodSignatureMapper.mapReturnType(this)}"

    private val IrDeclaration.parentsWithSelf: Sequence<IrDeclaration>
        get() = generateSequence(this) { it.parent as? IrDeclaration }

    private fun IrLocalDelegatedPropertyReference.findClassOwner(): IrClass {
        val containingClasses = symbol.owner.parentsWithSelf.filterIsInstance<IrClass>()
        // Prefer to attach metadata to non-synthetic classes, similarly to how it's done in rememberLocalProperty.
        return containingClasses.firstOrNull { !it.isSynthetic } ?: containingClasses.first()
    }

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
        val reference = IrRawFunctionReferenceImpl(startOffset, endOffset, expression.type, getter)
        reference.needsDummySignature = getter.owner.correspondingPropertySymbol?.owner?.needsAccessor(getter.owner) == false ||
                // Internal underlying vals of inline classes have no getter method
                getter.owner.isInlineClassFieldGetter && getter.owner.visibility == DescriptorVisibilities.INTERNAL
        return irCall(signatureStringIntrinsic).apply { arguments[0] = reference }
    }

    private fun IrClass.addOverride(method: IrSimpleFunction, buildBody: JvmIrBuilder.(List<IrValueParameter>) -> IrExpression) =
        addFunction {
            setSourceRange(this@addOverride)
            name = method.name
            returnType = method.returnType
            visibility = method.visibility
            modality = Modality.OPEN
            origin = JvmLoweredDeclarationOrigin.GENERATED_MEMBER_IN_CALLABLE_REFERENCE
        }.apply {
            overriddenSymbols += method.symbol
            parameters = listOf(thisReceiver!!.copyTo(this)) + method.nonDispatchParameters.map { it.copyTo(this) }
            body = context.createJvmIrBuilder(symbol, startOffset, endOffset).run {
                irExprBody(buildBody(parameters))
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
            parameters = listOf(thisReceiver!!.copyTo(this)) + method.nonDispatchParameters.map { it.copyTo(this) }
        }

    private class PropertyReferenceKind(
        val implSymbol: IrClassSymbol,
        val wrapper: IrFunction
    )

    private fun propertyReferenceKind(expression: IrCallableReference<*>, mutable: Boolean, i: Int): PropertyReferenceKind {
        check(i in 0..2) { "Incorrect number of receivers ($i) for property reference: ${expression.render()}" }
        val symbols = context.symbols
        return PropertyReferenceKind(
            symbols.getPropertyReferenceClass(mutable, i, true),
            symbols.reflection.owner.functions.single {
                it.name.asString() == (if (mutable) "mutableProperty$i" else "property$i")
            }
        )
    }

    private fun IrFunction.findExtensionReceiverParameter(): IrValueParameter? = parameters.find { it.kind == IrParameterKind.ExtensionReceiver }

    private fun IrCallableReference<*>.findExtensionReceiver(): IrExpression? {
        val extensionParameter = getter?.owner?.findExtensionReceiverParameter() ?: return null
        return arguments[extensionParameter.indexInParameters]
    }

    private fun propertyReferenceKindFor(expression: IrCallableReference<*>): PropertyReferenceKind =
        expression.getter?.owner?.let {
            val boundReceivers = listOfNotNull(expression.dispatchReceiver, expression.findExtensionReceiver()).size
            val needReceivers = listOfNotNull(it.dispatchReceiverParameter, it.findExtensionReceiverParameter()).size
            // PropertyReference1 will swap the receivers if bound with the extension one, and PropertyReference0
            // has no way to bind two receivers at once.
            check(boundReceivers < 2 && (expression.findExtensionReceiver() == null || needReceivers < 2)) {
                "Property reference with two receivers is not supported: ${expression.render()}"
            }
            propertyReferenceKind(expression, expression.setter != null, needReceivers - boundReceivers)
        } ?: expression.field?.owner?.let {
            propertyReferenceKind(expression, !it.isFinal, if (it.isStatic || expression.dispatchReceiver != null) 0 else 1)
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
            visibility =
                if (irClass.isInterface && context.config.jvmDefaultMode.isEnabled) DescriptorVisibilities.PUBLIC
                else JavaDescriptorVisibilities.PACKAGE_VISIBILITY
        }

        val localProperties = mutableListOf<IrLocalDelegatedPropertySymbol>()
        val localPropertyIndices = mutableMapOf<IrSymbol, Int>()

        fun localPropertyIndex(getter: IrSymbol): Int? =
            localPropertyIndices[getter] ?: parent?.localPropertyIndex(getter)

        fun rememberLocalProperty(property: IrLocalDelegatedProperty) {
            // Prefer to attach metadata to non-synthetic classes, because it won't be serialized otherwise;
            // if not possible, though, putting it right here will at least allow non-reflective uses.
            val metadataOwner = generateSequence(this) { it.parent }.find { !it.irClass.isSynthetic } ?: this
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
                initializer = context.createJvmIrBuilder(data.kPropertiesField.symbol).run {
                    val initializers = data.kProperties.values.sortedBy { it.index }.map { it.initializer }
                    irExprBody(irArrayOf(kPropertiesFieldType, initializers))
                }
            })
        }
        if (data.localProperties.isNotEmpty()) {
            declaration.localDelegatedProperties = data.localProperties
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

    private fun IrSimpleFunction.usesParameter(parameter: IrValueParameter): Boolean {
        parentClassId?.let { containerId ->
            // This function was imported from a jar. Didn't run the inline class lowering yet though - have to map manually.
            val replaced = context.inlineClassReplacements.getReplacementFunction(this) ?: this
            val signature = context.defaultMethodSignatureMapper.mapSignatureSkipGeneric(replaced)
            val hasDispatchReceiverAsInt = if (replaced.dispatchReceiverParameter != null) 1 else 0
            val nonDispatchIndex = parameter.indexInParameters - hasDispatchReceiverAsInt
            val localIndex = signature.parameters.take(nonDispatchIndex).sumOf { it.size } + hasDispatchReceiverAsInt
            // Null checks are removed during inlining, so we can ignore them.
            return loadCompiledInlineFunction(
                containerId,
                signature.asmMethod,
                isSuspend,
                hasMangledReturnType,
                context.evaluatorData != null && visibility == DescriptorVisibilities.INTERNAL,
                context.state
            ).node.usesLocalExceptParameterNullCheck(localIndex)
        }
        return hasChild { it is IrGetValue && it.symbol == parameters[parameter.indexInParameters].symbol }
    }

    // Assuming that the only functions that take PROPERTY_REFERENCE_FOR_DELEGATE-kind references are getValue,
    // setValue, and provideDelegate, there is only one valid index for each symbol, so we don't need it in the key.
    private val usesPropertyParameterCache = ConcurrentHashMap<IrSymbol, Boolean>()

    override fun visitCall(expression: IrCall): IrExpression {
        // Don't generate entries in `$$delegatedProperties` if they won't be used for anything. This is only possible
        // for inline functions, since for non-inline ones we need to provide some non-null value, and if they're not
        // in the same file, they can start using it without forcing a recompilation of this file.
        if (!expression.symbol.owner.isInline) return super.visitCall(expression)
        for (parameter in expression.symbol.owner.parameters) {
            val value = expression.arguments[parameter]
            if (value is IrCallableReference<*> && value.origin == IrStatementOrigin.PROPERTY_REFERENCE_FOR_DELEGATE) {
                val resolved = expression.symbol.owner.resolveFakeOverride() ?: expression.symbol.owner
                if (!usesPropertyParameterCache.getOrPut(resolved.symbol) { resolved.usesParameter(parameter) }) {
                    expression.arguments[parameter] = IrConstImpl.constNull(value.startOffset, value.endOffset, value.type)
                }
            }
        }
        return super.visitCall(expression)
    }

    private fun cachedKProperty(expression: IrCallableReference<*>): IrExpression {
        expression.transformChildrenVoid()
        if (expression.origin == REFLECTED_PROPERTY_REFERENCE)
            return createReflectedKProperty(expression)
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
                arguments[0] = irGetField(null, data.kPropertiesField)
                arguments[1] = irInt(index)
            }
        }
    }

    // Create an instance of KProperty that uses Java reflection to locate the getter and the setter. This kind of reference
    // does not support local variables and is slower, but takes up less space in the output binary.
    // Example: `C::property` -> `Reflection.property1(PropertyReference1Impl(C::class, "property", "getProperty()LType;"))`.
    private fun createReflectedKProperty(expression: IrCallableReference<*>): IrExpression {
        val boundReceiver = expression.getBoundReceiver()
        val referenceKind = propertyReferenceKindFor(expression)
        return context.createJvmIrBuilder(currentScope!!, expression).run {
            val arity = when {
                boundReceiver != null -> 5 // (receiver, jClass, name, desc, flags)
                else -> 4 // (jClass, name, desc, flags)
            }
            val instance = irCall(referenceKind.implSymbol.constructors.single { it.owner.parameters.size == arity }).apply {
                fillReflectedPropertyArguments(this, expression, boundReceiver)
            }
            irCall(referenceKind.wrapper).apply { arguments[0] = instance }
        }
    }

    private fun JvmIrBuilder.fillReflectedPropertyArguments(
        call: IrFunctionAccessExpression,
        expression: IrCallableReference<*>,
        receiver: IrExpression?,
    ) {
        val container = expression.propertyContainer
        val containerClass = kClassToJavaClass(calculateOwnerKClass(container))
        val isPackage = (container is IrClass && container.isFileClass) || container is IrPackageFragment
        call.arguments.assignFrom(
            listOfNotNull(
                receiver, containerClass,
                irString((expression.symbol.owner as IrDeclarationWithName).name.asString()),
                computeSignatureString(expression),
                irInt((if (isPackage) 1 else 0) or (if (expression.isJavaSyntheticPropertyReference) 2 else 0))
            )
        )
    }

    private val IrCallableReference<*>.isJavaSyntheticPropertyReference: Boolean
        get() =
            symbol.owner.let {
                it is IrProperty && it.backingField == null &&
                        (it.origin == IrDeclarationOrigin.SYNTHETIC_JAVA_PROPERTY_DELEGATE
                                || it.origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB)
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
                expression.getBoundReceiver()?.let { arguments[0] = it }
            }
        }
    }

    private fun createKPropertySubclass(expression: IrCallableReference<*>): IrClass {
        val superClass = propertyReferenceKindFor(expression).implSymbol.owner
        val referenceClass = context.irFactory.buildClass {
            setSourceRange(expression)
            name = SpecialNames.NO_NAME_PROVIDED
            origin = JvmLoweredDeclarationOrigin.GENERATED_PROPERTY_REFERENCE
            visibility = DescriptorVisibilities.LOCAL
        }.apply {
            parent = currentDeclarationParent!!
            superTypes = listOf(superClass.defaultType)
            createThisReceiverParameter()
            copyAttributes(expression)
        }

        addConstructor(expression, referenceClass, superClass)

        val boundReceiver = expression.getBoundReceiver()
        val get = superClass.functions.find { it.name.asString() == "get" }
        val set = superClass.functions.find { it.name.asString() == "set" }
        val invoke = superClass.functions.find { it.name.asString() == "invoke" }

        val field = expression.field?.owner
        if (field == null) {
            fun IrBuilderWithScope.setCallArguments(call: IrCall, parameters: List<IrValueParameter>) {
                val backingField =
                    with(FunctionReferenceLowering) { referenceClass.getReceiverField(this@PropertyReferenceLowering.context) }
                val receiverFromField = boundReceiver?.let { irImplicitCast(irGetField(irGet(parameters[0]), backingField), it.type) }
                if (expression.isJavaSyntheticPropertyReference) {
                    assert(call.typeArguments.size == 0) { "Unexpected type arguments: ${call.typeArguments.size}" }
                } else {
                    call.copyTypeArgumentsFrom(expression)
                }

                call.symbol.owner.dispatchReceiverParameter?.let {
                    call.arguments[it] = receiverFromField ?: irImplicitCast(irGet(parameters[1]), expression.receiverType)
                }
                call.symbol.owner.findExtensionReceiverParameter()?.let {
                    call.arguments[it] = if (call.symbol.owner.dispatchReceiverParameter == null)
                        receiverFromField ?: irImplicitCast(irGet(parameters[1]), it.type)
                    else
                        irImplicitCast(irGet(parameters[if (receiverFromField != null) 1 else 2]), it.type)
                }
            }

            expression.getter?.owner?.let { getter ->
                referenceClass.addOverride(get!!) { parameters ->
                    expression.constInitializer?.let { return@addOverride it }
                    irCall(getter, origin = IrStatementOrigin.GET_PROPERTY).apply {
                        setCallArguments(this, parameters)
                    }
                }
                referenceClass.addFakeOverride(invoke!!)
            }

            expression.setter?.owner?.let { setter ->
                referenceClass.addOverride(set!!) { parameters ->
                    irCall(setter, origin = IrStatementOrigin.EQ).apply {
                        setCallArguments(this, parameters)
                        arguments[arguments.lastIndex] = irGet(parameters.last())
                    }
                }
            }
        } else {
            fun IrBuilderWithScope.fieldReceiver(arguments: List<IrValueParameter>) = when {
                field.isStatic ->
                    null
                expression.dispatchReceiver != null -> {
                    val backingField =
                        with(FunctionReferenceLowering) { referenceClass.getReceiverField(this@PropertyReferenceLowering.context) }
                    irImplicitCast(irGetField(irGet(arguments[0]), backingField), expression.receiverType)
                }
                else ->
                    irImplicitCast(irGet(arguments[1]), expression.receiverType)
            }

            referenceClass.addOverride(get!!) { arguments ->
                expression.constInitializer?.let { return@addOverride it }
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
        val hasBoundReceiver = expression.getBoundReceiver() != null
        val numOfSuperArgs = (if (hasBoundReceiver) 1 else 0) + 4
        val superConstructor = superClass.constructors.single { it.parameters.size == numOfSuperArgs }

        referenceClass.addConstructor {
            origin = JvmLoweredDeclarationOrigin.GENERATED_MEMBER_IN_CALLABLE_REFERENCE
            isPrimary = true
        }.apply {
            val receiverParameter = if (hasBoundReceiver) addValueParameter("receiver", context.irBuiltIns.anyNType) else null
            body = context.createJvmIrBuilder(symbol).run {
                irBlockBody(startOffset, endOffset) {
                    +irDelegatingConstructorCall(superConstructor).apply {
                        fillReflectedPropertyArguments(this, expression, receiverParameter?.let(::irGet))
                    }
                    +IrInstanceInitializerCallImpl(startOffset, endOffset, referenceClass.symbol, context.irBuiltIns.unitType)
                }
            }
        }
    }

    // In `value::x`, using `value`'s type is fine; but in `C::x`, the type of the receiver has to be `C`.
    // This is *not* the type of `x`'s dispatch receiver if `x` is declared in a superclass of `C`, so we
    // extract `C` from the reference's type, which is either `KProperty1<C, R>` or `KProperty2<C, Extension, R>`.
    private val IrCallableReference<*>.receiverType
        get() = dispatchReceiver?.type ?: ((type as IrSimpleType).arguments.first() as IrTypeProjection).type

    private fun IrCallableReference<*>.getBoundReceiver(): IrExpression? {
        val callee = symbol.owner
        return when {
            callee is IrDeclaration && callee.isJvmStaticInObject() -> {
                // See FunctionReferenceLowering.FunctionReferenceBuilder.createFakeBoundReceiverForJvmStaticInObject.
                val objectClass = callee.parentAsClass
                IrGetObjectValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, objectClass.typeWith(), objectClass.symbol)
            }
            callee is IrProperty
                    && callee.getter?.origin == IrDeclarationOrigin.DELEGATED_PROPERTY_ACCESSOR
                    && callee.getter?.dispatchReceiverParameter?.origin == IrDeclarationOrigin.SCRIPT_THIS_RECEIVER -> {
                // without this exception, the PropertyReferenceLowering generates `clinit` with an attempt to use script as receiver
                // TODO: find whether it is a valid exception and maybe how to make it more obvious (KT-72942)
                null
            }
            else -> dispatchReceiver ?: findExtensionReceiver()
        }
    }
}

/**
 * An origin for IrFunctionReferences which prevents inline class mangling. This only exists because of
 * inconsistencies between `RuntimeTypeMapper` and `KotlinTypeMapper`. The `RuntimeTypeMapper` does not
 * perform inline class mangling and so in the absence of jvm signatures in the metadata we need to avoid
 * inline class mangling as well in the function references used as arguments to the signature string intrinsic.
 */
internal var IrRawFunctionReference.needsDummySignature by irFlag(copyByDefault = true)
    private set
