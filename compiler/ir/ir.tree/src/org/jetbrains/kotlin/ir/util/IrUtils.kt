/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrPropertyImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrTypeParameterImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.source.PsiSourceElement
import org.jetbrains.kotlin.types.KotlinType

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

//fun IrFunction.createParameterDeclarations() {
//    fun ParameterDescriptor.irValueParameter() = IrValueParameterImpl(
//        innerStartOffset(this), innerEndOffset(this),
//        IrDeclarationOrigin.DEFINED,
//        this
//    ).also {
//        it.parent = this@createParameterDeclarations
//    }
//
//    dispatchReceiverParameter = descriptor.dispatchReceiverParameter?.irValueParameter()
//    extensionReceiverParameter = descriptor.extensionReceiverParameter?.irValueParameter()
//
//    assert(valueParameters.isEmpty())
//    descriptor.valueParameters.mapTo(valueParameters) { it.irValueParameter() }
//
//    assert(typeParameters.isEmpty())
//    descriptor.typeParameters.mapTo(typeParameters) {
//        IrTypeParameterImpl(
//            innerStartOffset(it), innerEndOffset(it),
//            IrDeclarationOrigin.DEFINED,
//            it
//        ).also { typeParameter ->
//            typeParameter.parent = this
//        }
//    }
//}
//
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

val IrClassSymbol.constructors: Sequence<IrConstructorSymbol>
    get() = this.owner.declarations.asSequence().filterIsInstance<IrConstructor>().map { it.symbol }

val IrFunction.explicitParameters: List<IrValueParameterSymbol>
    get() = (listOfNotNull(dispatchReceiverParameter, extensionReceiverParameter) + valueParameters).map { it.symbol }

val IrClass.defaultType: KotlinType
    get() = this.descriptor.defaultType

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
