/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrTypeParameterImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.source.PsiSourceElement

/**
 * Binds the arguments explicitly represented in the IR to the parameters of the accessed function.
 * The arguments are to be evaluated in the same order as they appear in the resulting list.
 */
fun IrMemberAccessExpression.getArguments(): List<Pair<ParameterDescriptor, IrExpression>> {
    val res = mutableListOf<Pair<ParameterDescriptor, IrExpression>>()
    val descriptor = descriptor

    // TODO: ensure the order below corresponds to the one defined in Kotlin specs.

    dispatchReceiver?.let {
        res += (descriptor.dispatchReceiverParameter!! to it)
    }

    extensionReceiver?.let {
        res += (descriptor.extensionReceiverParameter!! to it)
    }

    descriptor.valueParameters.forEach {
        val arg = getValueArgument(it.index)
        if (arg != null) {
            res += (it to arg)
        }
    }

    return res
}

/**
 * Binds the arguments explicitly represented in the IR to the parameters of the accessed function.
 * The arguments are to be evaluated in the same order as they appear in the resulting list.
 */
fun IrFunctionAccessExpression.getArgumentsWithSymbols(): List<Pair<IrValueParameterSymbol, IrExpression>> {
    val res = mutableListOf<Pair<IrValueParameterSymbol, IrExpression>>()
    val irFunction = symbol.owner as IrFunction

    dispatchReceiver?.let {
        res += (irFunction.dispatchReceiverParameter!!.symbol to it)
    }

    extensionReceiver?.let {
        res += (irFunction.extensionReceiverParameter!!.symbol to it)
    }

    irFunction.valueParameters.forEach {
        val arg = getValueArgument(it.descriptor as ValueParameterDescriptor)
        if (arg != null) {
            res += (it.symbol to arg)
        }
    }

    return res
}

/**
 * Binds the arguments explicitly represented in the IR to the parameters of the accessed function.
 * The arguments are to be evaluated in the same order as they appear in the resulting list.
 */
fun IrMemberAccessExpression.getArgumentsWithIr(): List<Pair<IrValueParameter, IrExpression>> {
    val res = mutableListOf<Pair<IrValueParameter, IrExpression>>()
    val irFunction = when (this) {
        is IrFunctionAccessExpression -> this.symbol.owner
        is IrFunctionReference -> this.symbol.owner
        else -> error(this)
    }

    dispatchReceiver?.let {
        res += (irFunction.dispatchReceiverParameter!! to it)
    }

    extensionReceiver?.let {
        res += (irFunction.extensionReceiverParameter!! to it)
    }

    irFunction.valueParameters.forEachIndexed { index, it ->
        val arg = getValueArgument(index)
        if (arg != null) {
            res += (it to arg)
        }
    }

    return res
}

/**
 * Sets arguments that are specified by given mapping of parameters.
 */
fun IrMemberAccessExpression.addArguments(args: Map<ParameterDescriptor, IrExpression>) {
    descriptor.dispatchReceiverParameter?.let {
        val arg = args[it]
        if (arg != null) {
            this.dispatchReceiver = arg
        }
    }

    descriptor.extensionReceiverParameter?.let {
        val arg = args[it]
        if (arg != null) {
            this.extensionReceiver = arg
        }
    }

    descriptor.valueParameters.forEach {
        val arg = args[it]
        if (arg != null) {
            this.putValueArgument(it.index, arg)
        }
    }
}

fun IrMemberAccessExpression.addArguments(args: List<Pair<ParameterDescriptor, IrExpression>>) =
    this.addArguments(args.toMap())

fun IrExpression.isNullConst() = this is IrConst<*> && this.kind == IrConstKind.Null

fun IrMemberAccessExpression.usesDefaultArguments(): Boolean =
    this.descriptor.valueParameters.any { this.getValueArgument(it) == null }

fun IrFunction.createParameterDeclarations() {
    fun ParameterDescriptor.irValueParameter() = IrValueParameterImpl(
        innerStartOffset(this), innerEndOffset(this),
        IrDeclarationOrigin.DEFINED,
        this,
        type.toIrType()!!,
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
            innerStartOffset(it), innerEndOffset(it),
            IrDeclarationOrigin.DEFINED,
            it
        ).also { typeParameter ->
            typeParameter.parent = this
        }
    }
}

fun IrClass.createParameterDeclarations() {
    thisReceiver = IrValueParameterImpl(
        startOffset, endOffset,
        IrDeclarationOrigin.INSTANCE_RECEIVER,
        descriptor.thisAsReceiverParameter,
        this.symbol.typeWith(this.typeParameters.map { it.defaultType }),
        null
    ).also { valueParameter ->
        valueParameter.parent = this
    }

    assert(typeParameters.isEmpty())
    assert(descriptor.declaredTypeParameters.isEmpty())
}

//fun IrClass.createParameterDeclarations() {
//    descriptor.thisAsReceiverParameter.let {
//        thisReceiver = IrValueParameterImpl(
//            innerStartOffset(it), innerEndOffset(it),
//            IrDeclarationOrigin.INSTANCE_RECEIVER,
//            it
//        )
//    }
//
//    assert(typeParameters.isEmpty())
//    descriptor.declaredTypeParameters.mapTo(typeParameters) {
//        IrTypeParameterImpl(
//            innerStartOffset(it), innerEndOffset(it),
//            IrDeclarationOrigin.DEFINED,
//            it
//        )
//    }
//}
//
//fun IrClass.addFakeOverrides() {
//
//    val startOffset = this.startOffset
//    val endOffset = this.endOffset
//
//    fun FunctionDescriptor.createFunction(): IrSimpleFunction = IrFunctionImpl(
//        startOffset, endOffset,
//        IrDeclarationOrigin.FAKE_OVERRIDE, this
//    ).apply {
//        createParameterDeclarations()
//    }
//
//    descriptor.unsubstitutedMemberScope.getContributedDescriptors()
//        .filterIsInstance<CallableMemberDescriptor>()
//        .filter { it.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE }
//        .mapTo(this.declarations) {
//            when (it) {
//                is FunctionDescriptor -> it.createFunction()
//                is PropertyDescriptor ->
//                    IrPropertyImpl(startOffset, endOffset, IrDeclarationOrigin.FAKE_OVERRIDE, it).apply {
//                        // TODO: add field if getter is missing?
//                        getter = it.getter?.createFunction()
//                        setter = it.setter?.createFunction()
//                    }
//                else -> TODO(it.toString())
//            }
//        }
//}

private fun IrElement.innerStartOffset(descriptor: DeclarationDescriptorWithSource): Int =
    descriptor.startOffset ?: this.startOffset

private fun IrElement.innerEndOffset(descriptor: DeclarationDescriptorWithSource): Int =
    descriptor.endOffset ?: this.endOffset

val DeclarationDescriptorWithSource.startOffset: Int? get() = (this.source as? PsiSourceElement)?.psi?.startOffset
val DeclarationDescriptorWithSource.endOffset: Int? get() = (this.source as? PsiSourceElement)?.psi?.endOffset

val DeclarationDescriptorWithSource.startOffsetOrUndefined: Int get() = startOffset ?: UNDEFINED_OFFSET
val DeclarationDescriptorWithSource.endOffsetOrUndefined: Int get() = endOffset ?: UNDEFINED_OFFSET

val IrClassSymbol.functions: Sequence<IrSimpleFunctionSymbol>
    get() = this.owner.declarations.asSequence().filterIsInstance<IrSimpleFunction>().map { it.symbol }

val IrClass.functions: Sequence<IrSimpleFunction>
    get() = this.declarations.asSequence().filterIsInstance<IrSimpleFunction>()

val IrClassSymbol.constructors: Sequence<IrConstructorSymbol>
    get() = this.owner.declarations.asSequence().filterIsInstance<IrConstructor>().map { it.symbol }

val IrClass.constructors: Sequence<IrConstructor>
    get() = this.declarations.asSequence().filterIsInstance<IrConstructor>()

val IrFunction.explicitParameters: List<IrValueParameter>
    get() = (listOfNotNull(dispatchReceiverParameter, extensionReceiverParameter) + valueParameters)

val IrClass.defaultType: IrType
    get() = this.thisReceiver!!.type

val IrSimpleFunction.isReal: Boolean get() = descriptor.kind.isReal

// This implementation is from kotlin-native
// TODO: use this implementation instead of any other
fun IrSimpleFunction.resolveFakeOverride(): IrSimpleFunction? {

    if (isReal) return this

    val visited = mutableSetOf<IrSimpleFunction>()
    val realOverrides = mutableSetOf<IrSimpleFunction>()

    fun collectRealOverrides(func: IrSimpleFunction) {
        if (!visited.add(func)) return

        if (func.isReal) {
            realOverrides += func
        } else {
            func.overriddenSymbols.forEach { collectRealOverrides(it.owner) }
        }
    }

    overriddenSymbols.forEach { collectRealOverrides(it.owner) }

    fun excludeRepeated(func: IrSimpleFunction) {
        if (!visited.add(func)) return

        func.overriddenSymbols.forEach {
            realOverrides.remove(it.owner)
            excludeRepeated(it.owner)
        }
    }

    visited.clear()
    realOverrides.toList().forEach { excludeRepeated(it) }

    return realOverrides.singleOrNull { it.modality != Modality.ABSTRACT }
}

val IrClass.isAnnotationClass get() = kind == ClassKind.ANNOTATION_CLASS
val IrClass.isEnumClass get() = kind == ClassKind.ENUM_CLASS
val IrClass.isEnumEntry get() = kind == ClassKind.ENUM_ENTRY
val IrClass.isInterface get() = kind == ClassKind.INTERFACE
val IrClass.isClass get() = kind == ClassKind.CLASS
val IrClass.isObject get() = kind == ClassKind.OBJECT

val IrDeclaration.parentAsClass get() = parent as IrClass

fun IrAnnotationContainer.hasAnnotation(name: FqName) =
    annotations.any {
        it.symbol.owner.parentAsClass.descriptor.fqNameSafe == name
    }

val IrConstructor.constructedClassType get() = (parent as IrClass).thisReceiver?.type!!

fun IrFunction.isFakeOverriddenFromAny(): Boolean {
    if (origin != IrDeclarationOrigin.FAKE_OVERRIDE) {
        return (parent as? IrClass)?.thisReceiver?.type?.isAny() ?: false
    }

    return (this as IrSimpleFunction).overriddenSymbols.all { it.owner.isFakeOverriddenFromAny() }
}

fun IrCall.isSuperToAny() = superQualifier?.let { this.symbol.owner.isFakeOverriddenFromAny() } ?: false

fun IrDeclaration.isEffectivelyExternal(): Boolean {
    return when (this) {
        is IrFunction -> isExternal || parent is IrDeclaration && parent.isEffectivelyExternal()
        is IrField -> isExternal || parent is IrDeclaration && parent.isEffectivelyExternal()
        is IrClass -> isExternal || parent is IrDeclaration && parent.isEffectivelyExternal()
        else -> false
    }
}

fun IrDeclaration.isDynamic() = this is IrFunction && dispatchReceiverParameter?.type is IrDynamicType

fun IrValueParameter.copy(newDescriptor: ParameterDescriptor): IrValueParameter {
    assert(this.descriptor.type == newDescriptor.type)

    return IrValueParameterImpl(
        startOffset,
        endOffset,
        IrDeclarationOrigin.DEFINED,
        newDescriptor,
        type,
        varargElementType
    )
}

fun createField(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    name: Name,
    isMutable: Boolean,
    origin: IrDeclarationOrigin,
    owner: ClassDescriptor
): IrField {
    val descriptor = PropertyDescriptorImpl.create(
        /* containingDeclaration = */ owner,
        /* annotations           = */ Annotations.EMPTY,
        /* modality              = */ Modality.FINAL,
        /* visibility            = */ Visibilities.PRIVATE,
        /* isVar                 = */ isMutable,
        /* name                  = */ name,
        /* kind                  = */ CallableMemberDescriptor.Kind.DECLARATION,
        /* source                = */ SourceElement.NO_SOURCE,
        /* lateInit              = */ false,
        /* isConst               = */ false,
        /* isExpect              = */ false,
        /* isActual                = */ false,
        /* isExternal            = */ false,
        /* isDelegated           = */ false
    ).apply {
        initialize(null, null)

        setType(type.toKotlinType(), emptyList(), owner.thisAsReceiverParameter, null)
    }

    return IrFieldImpl(startOffset, endOffset, origin, descriptor, type)
}

fun IrFunction.createDispatchReceiverParameter() {
    assert(this.dispatchReceiverParameter == null)

    val descriptor = this.descriptor.dispatchReceiverParameter ?: return

    this.dispatchReceiverParameter = IrValueParameterImpl(
        startOffset,
        endOffset,
        IrDeclarationOrigin.DEFINED,
        descriptor,
        this.parentAsClass.defaultType,
        null
    ).also { it.parent = this }
}

fun ReferenceSymbolTable.referenceClassifier(classifier: ClassifierDescriptor): IrClassifierSymbol =
    when (classifier) {
        is TypeParameterDescriptor ->
            referenceTypeParameter(classifier)
        is ClassDescriptor ->
            referenceClass(classifier)
        else ->
            throw IllegalArgumentException("Unexpected classifier descriptor: $classifier")
    }

fun ReferenceSymbolTable.referenceFunction(callable: CallableDescriptor): IrFunctionSymbol =
    when (callable) {
        is ClassConstructorDescriptor ->
            referenceConstructor(callable)
        is FunctionDescriptor ->
            referenceSimpleFunction(callable)
        else ->
            throw IllegalArgumentException("Unexpected callable descriptor: $callable")
    }
