/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.PhasePrerequisites
import org.jetbrains.kotlin.backend.jvm.*
import org.jetbrains.kotlin.backend.jvm.ir.*
import org.jetbrains.kotlin.backend.jvm.lower.FunctionReferenceLowering.Companion.calculateOwnerKClass
import org.jetbrains.kotlin.backend.jvm.lower.PropertyReferenceLowering.PropertyReferenceTarget.*
import org.jetbrains.kotlin.codegen.inline.loadCompiledInlineFunction
import org.jetbrains.kotlin.codegen.optimization.nullCheck.usesLocalExceptParameterNullCheck
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrAttribute
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin.Companion.PROPERTY_REFERENCE_FOR_DELEGATE
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrRawFunctionReferenceImpl
import org.jetbrains.kotlin.ir.irFlag
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrDeclarationWithAccessorsSymbol
import org.jetbrains.kotlin.ir.symbols.IrLocalDelegatedPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.createType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrTransformer
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
@PhasePrerequisites(
    // This must be done after contents of functions are extracted into separate classes, or else the `$$delegatedProperties`
    // field will end up in the wrong class (not the one that declares the delegated property).
    FunctionReferenceLowering::class,
    SuspendLambdaLowering::class,
    PropertyReferenceDelegationLowering::class,
)
internal class PropertyReferenceLowering(val context: JvmBackendContext) : IrElementTransformerVoidWithContext(), FileLoweringPass {
    companion object {
        // Marking a property reference with this origin causes it to not generate a class.
        val REFLECTED_PROPERTY_REFERENCE by IrStatementOriginImpl
    }

    private val IrRichPropertyReference.symbol: IrDeclarationWithAccessorsSymbol get() = reflectionTargetSymbol!!

    private val IrRichPropertyReference.property: IrProperty
        get() = (symbol.owner as IrProperty)

    private val IrRichPropertyReference.localDelegatedProperty: IrLocalDelegatedProperty
        get() = symbol.owner as IrLocalDelegatedProperty

    private val IrRichPropertyReference.originalGetter: IrSimpleFunction?
        get() =
            if (isLocalDelegatedPropertyReference) localDelegatedProperty.getter
            else property.getter?.let { it.resolveFakeOverride() ?: it }

    private val IrRichPropertyReference.originalSetter: IrSimpleFunction?
        get() =
            if (isLocalDelegatedPropertyReference) localDelegatedProperty.setter
            else property.setter?.let { it.resolveFakeOverride() ?: it }

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

    private val IrRichPropertyReference.isLocalDelegatedPropertyReference: Boolean
        get() = this.reflectionTargetSymbol is IrLocalDelegatedPropertySymbol

    private val IrRichPropertyReference.propertyContainer: IrDeclarationParent
        get() = if (isLocalDelegatedPropertyReference) {
            val containingClasses = localDelegatedProperty.parentsWithSelf.filterIsInstance<IrClass>()
            // Prefer to attach metadata to non-synthetic classes, similarly to how it's done in rememberLocalProperty.
            containingClasses.firstOrNull { !it.isSynthetic } ?: containingClasses.first()
        } else {
            property.parent
        }

    // Plain Java fields do not have a getter, but can be referenced nonetheless.
    // The signature should be the one that a getter would have, if it existed.
    private fun fakeGetterSignatureFor(name: String, typeHolder: IrDeclaration) =
        "${JvmAbi.getterName(name)}()${context.defaultMethodSignatureMapper.mapReturnType(typeHolder)}"

    private val IrDeclaration.parentsWithSelf: Sequence<IrDeclaration>
        get() = generateSequence(this) { it.parent as? IrDeclaration }

    override fun visitRichPropertyReference(expression: IrRichPropertyReference): IrExpression = cachedKProperty(expression)

    private fun IrBuilderWithScope.computeSignatureString(expression: IrRichPropertyReference): IrExpression {
        if (expression.isLocalDelegatedPropertyReference) {
            // Local delegated properties are stored as a plain list, and the runtime library extracts the index from this string:
            val index = currentClassData?.localPropertyIndex(expression.originalGetter!!.symbol)
                ?: throw AssertionError("local property reference before declaration: ${expression.render()}")
            return irString("<v#$index>")
        }
        val getter = expression.originalGetter ?: return irString(fakeGetterSignatureFor(expression.property.name.asString(), expression.getterFunction))
        // Work around for differences between `RuntimeTypeMapper.KotlinProperty` and the real Kotlin type mapper.
        // Most notably, the runtime type mapper does not perform inline class name mangling. This is usually not
        // a problem, since we will produce a getter signature as part of the Kotlin metadata, except when there
        // is no getter method in the bytecode. In that case we need to avoid inline class mangling for the
        // function reference used in the <signature-string> intrinsic.
        //
        // Note that we cannot compute the signature at this point, since we still need to mangle the names of
        // private properties in multifile-part classes.
        val reference = IrRawFunctionReferenceImpl(startOffset, endOffset, expression.type, getter.symbol)
        reference.needsDummySignature = getter.correspondingPropertySymbol?.owner?.needsAccessor(getter) == false ||
                // Internal underlying vals of inline classes have no getter method
                getter.isInlineClassFieldGetter && getter.visibility == DescriptorVisibilities.INTERNAL
        return irCall(signatureStringIntrinsic).apply { arguments[0] = reference }
    }

    private fun IrClass.addOverride(method: IrSimpleFunction, buildBody: JvmIrBuilder.(IrSimpleFunction) -> IrBody) =
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
            body = context.createJvmIrBuilder(symbol, startOffset, endOffset).buildBody(this@apply)
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

    private fun propertyReferenceClassFor(expression: IrRichPropertyReference): IrClassSymbol {
        val boundReceivers = expression.getBoundValues(REFLECTED_PROPERTY)
        val getterFunction = expression.originalGetter ?: expression.getterFunction
        val needReceiversCount = getterFunction.parameters.size + if (getterFunction.isJvmStaticInObject()) 1 else 0
        check(boundReceivers.size < 2 && boundReceivers.size <= needReceiversCount) {
            "Property reference with two and more receivers is not supported: ${expression.dump()}"
        }
        val mutable = expression.setterFunction != null
        val i = needReceiversCount - boundReceivers.size
        check(i in 0..2) { "Incorrect number of receivers ($i) for property reference: ${expression.render()}" }
        return context.symbols.getPropertyReferenceClass(mutable, i, true)
    }

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
            if (value is IrRichPropertyReference && value.origin == PROPERTY_REFERENCE_FOR_DELEGATE) {
                val resolved = expression.symbol.owner.resolveFakeOverride() ?: expression.symbol.owner
                if (!usesPropertyParameterCache.getOrPut(resolved.symbol) { resolved.usesParameter(parameter) }) {
                    expression.arguments[parameter] = IrConstImpl.constNull(value.startOffset, value.endOffset, value.type)
                }
            }
        }
        return super.visitCall(expression)
    }

    private fun cachedKProperty(expression: IrRichPropertyReference): IrExpression {
        expression.transformChildrenVoid()
        return when (expression.origin) {
            REFLECTED_PROPERTY_REFERENCE -> createReflectedKProperty(expression)
            PROPERTY_REFERENCE_FOR_DELEGATE -> createKPropertyReferenceForDelegate(expression)
            else -> createSpecializedKProperty(expression)
        }
    }

    private fun createKPropertyReferenceForDelegate(expression: IrRichPropertyReference): IrFunctionAccessExpression {
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
    private fun createReflectedKProperty(expression: IrRichPropertyReference): IrExpression {
        val boundReceivers = expression.getBoundValues(REFLECTED_PROPERTY)
        require(boundReceivers.size <= 1) { "Property references can not capture more than one receiver: ${expression.dump()}" }
        val boundReceiver = boundReceivers.firstOrNull()
        val referenceClass = propertyReferenceClassFor(expression)
        return context.createJvmIrBuilder(currentScope!!, expression).run {
            val arity = when {
                boundReceiver != null -> 5 // (receiver, jClass, name, desc, flags)
                else -> 4 // (jClass, name, desc, flags)
            }
            irCall(referenceClass.constructors.single { it.owner.parameters.size == arity }).apply {
                fillReflectedPropertyArguments(this, expression, boundReceiver?.let(expression.boundValues::get))
            }
        }
    }

    private fun JvmIrBuilder.fillReflectedPropertyArguments(
        call: IrFunctionAccessExpression,
        expression: IrRichPropertyReference,
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

    private val IrRichPropertyReference.isJavaSyntheticPropertyReference: Boolean
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
    private fun createSpecializedKProperty(expression: IrRichPropertyReference): IrExpression {
        return context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol, expression.startOffset, expression.endOffset).irBlock {
            val propertyBoundValues = expression.getBoundValues(REFLECTED_PROPERTY)
            val getterBoundValues = expression.getBoundValues(GETTER)
            val setterBoundValues = expression.getBoundValues(SETTER)
            // We do not reuse classes for non-reflective property references because they would not have
            // a valid enclosing method if the same property is referenced at many points.
            val referenceClass = createKPropertySubclass(expression, getterBoundValues, setterBoundValues)
            +referenceClass
            +irCall(referenceClass.constructors.single()).apply {
                arguments.assignFrom(propertyBoundValues) { expression.boundValues[it] }
            }
        }
    }

    private fun createKPropertySubclass(
        expression: IrRichPropertyReference,
        getterBoundValues: List<Int>,
        setterBoundValues: List<Int>,
    ): IrClass {
        val superClass = propertyReferenceClassFor(expression).owner
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

        val get = superClass.functions.find { it.name.asString() == "get" }
        val set = superClass.functions.find { it.name.asString() == "set" }
        val invoke = superClass.functions.find { it.name.asString() == "invoke" }

        fun IrBuilder.getArguments(boundParameters: List<Int>, function: IrSimpleFunction): List<() -> IrExpression> {
            require(boundParameters.size <= 1) { "Property references can not capture more than one receiver: ${function.dump()}" }
            val boundExpressions = boundParameters.map {
                {
                    val field = with(FunctionReferenceLowering) {
                        referenceClass.getReceiverField(this@PropertyReferenceLowering.context)
                    }
                    irGetField(irGet(function.dispatchReceiverParameter!!), field)
                }
            }
            val unboundExpressions = function.nonDispatchParameters.map { { irGet(it) } }
            return boundExpressions + unboundExpressions
        }

        expression.getterFunction.let { getter ->
            referenceClass.addOverride(get!!) { function ->
                expression.constInitializer?.let { return@addOverride irExprBody(it) }
                val arguments = getArguments(getterBoundValues, function)
                getter.inlineWithoutTemporaryVariables(function, arguments)
            }
            referenceClass.addFakeOverride(invoke!!)
        }

        expression.setterFunction?.let { setter ->
            referenceClass.addOverride(set!!) { function ->
                val arguments = getArguments(setterBoundValues, function)
                setter.inlineWithoutTemporaryVariables(function, arguments)
            }
        }

        return referenceClass
    }

    private fun IrFunction.inlineWithoutTemporaryVariables(target: IrFunction, arguments: List<() -> IrExpression>): IrBody {
        val mapping = parameters.zip(arguments).toMap()
        val source = this
        return body!!.transform(object : IrTransformer<Nothing?>() {
            override fun visitGetValue(expression: IrGetValue, data: Nothing?): IrExpression =
                mapping[expression.symbol.owner]?.invoke()?.implicitCastIfNeededTo(expression.type) ?: expression

            override fun visitReturn(expression: IrReturn, data: Nothing?): IrExpression {
                if (expression.returnTargetSymbol == source.symbol) {
                    expression.returnTargetSymbol = target.symbol
                }
                return super.visitReturn(expression, data)
            }

            override fun visitDeclaration(declaration: IrDeclarationBase, data: Nothing?): IrStatement {
                if (declaration.parent == source) {
                    declaration.parent = target
                }
                return super.visitDeclaration(declaration, data)
            }
        }, null)
    }

    private fun addConstructor(expression: IrRichPropertyReference, referenceClass: IrClass, superClass: IrClass) {
        val hasBoundReceiver = expression.getBoundValues(REFLECTED_PROPERTY).isNotEmpty()
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

    private enum class PropertyReferenceTarget { REFLECTED_PROPERTY, GETTER, SETTER }

    /**
     * Retrieves the indices of bound values for a given property reference target.
     *
     * [propertyReferenceTarget] is necessary because [IrDeclaration.isJvmStaticInObject] accessors do not have a dispatch receiver parameter.
     *
     * @param propertyReferenceTarget Specifies the target of the property reference, which can be a reflected property, getter, or setter.
     * @return The bound value indices of the property reference.
     */
    private fun IrRichPropertyReference.getBoundValues(propertyReferenceTarget: PropertyReferenceTarget): List<Int> {
        val removeBoundReceiver = when (propertyReferenceTarget) {
            GETTER -> false
            SETTER -> false
            REFLECTED_PROPERTY -> {
                val callee = if (isLocalDelegatedPropertyReference) localDelegatedProperty else property
                // without this exception, the PropertyReferenceLowering generates `clinit` with an attempt to use script as receiver
                // TODO: find whether it is a valid exception and maybe how to make it more obvious (KT-72942)
                callee is IrProperty
                        && callee.getter?.origin == IrDeclarationOrigin.DELEGATED_PROPERTY_ACCESSOR
                        && callee.getter?.dispatchReceiverParameter?.origin == IrDeclarationOrigin.SCRIPT_THIS_RECEIVER
            }
        }

        return boundValues.indices.drop(if (removeBoundReceiver) 1 else 0)
    }
}

/**
 * An [IrAttribute] for [IrRawFunctionReference]s which prevents inline class mangling. This only exists because of
 * inconsistencies between `RuntimeTypeMapper` and `KotlinTypeMapper`. The `RuntimeTypeMapper` does not
 * perform inline class mangling and so in the absence of jvm signatures in the metadata we need to avoid
 * inline class mangling as well in the function references used as arguments to the signature string intrinsic.
 */
internal var IrRawFunctionReference.needsDummySignature by irFlag(copyByDefault = true)
    private set
