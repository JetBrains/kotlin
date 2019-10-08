/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.source.PsiSourceElement
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.utils.DFS

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
    val irFunction = symbol.owner

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
        is IrPropertyReference -> {
            assert(this.field == null) { "Field should be null to use `getArgumentsWithIr` on IrPropertyReference: ${this.dump()}}" }
            this.getter!!.owner
        }
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

fun IrExpression.isTrueConst() = this is IrConst<*> && this.kind == IrConstKind.Boolean && this.value == true

fun IrExpression.isFalseConst() = this is IrConst<*> && this.kind == IrConstKind.Boolean && this.value == false

fun IrExpression.coerceToUnitIfNeeded(valueType: KotlinType, irBuiltIns: IrBuiltIns): IrExpression {
    return if (KotlinTypeChecker.DEFAULT.isSubtypeOf(valueType, irBuiltIns.unitType.toKotlinType()))
        this
    else
        IrTypeOperatorCallImpl(
            startOffset, endOffset,
            irBuiltIns.unitType,
            IrTypeOperator.IMPLICIT_COERCION_TO_UNIT,
            irBuiltIns.unitType,
            this
        )
}

fun IrExpression.coerceToUnitIfNeeded(valueType: IrType, irBuiltIns: IrBuiltIns): IrExpression {
    return if (valueType.isSubtypeOf(irBuiltIns.unitType, irBuiltIns))
        this
    else
        IrTypeOperatorCallImpl(
            startOffset, endOffset,
            irBuiltIns.unitType,
            IrTypeOperator.IMPLICIT_COERCION_TO_UNIT,
            irBuiltIns.unitType,
            this
        )
}

fun IrMemberAccessExpression.usesDefaultArguments(): Boolean =
    this.descriptor.valueParameters.any { this.getValueArgument(it) == null }

val DeclarationDescriptorWithSource.startOffset: Int? get() = (this.source as? PsiSourceElement)?.psi?.startOffset
val DeclarationDescriptorWithSource.endOffset: Int? get() = (this.source as? PsiSourceElement)?.psi?.endOffset

val IrClass.functions: Sequence<IrSimpleFunction>
    get() = declarations.asSequence().filterIsInstance<IrSimpleFunction>()

val IrClassSymbol.functions: Sequence<IrSimpleFunctionSymbol>
    get() = owner.functions.map { it.symbol }

val IrClass.constructors: Sequence<IrConstructor>
    get() = declarations.asSequence().filterIsInstance<IrConstructor>()

val IrClassSymbol.constructors: Sequence<IrConstructorSymbol>
    get() = owner.constructors.map { it.symbol }

val IrClass.fields: Sequence<IrField>
    get() = declarations.asSequence().filterIsInstance<IrField>()

val IrClassSymbol.fields: Sequence<IrFieldSymbol>
    get() = owner.fields.map { it.symbol }

val IrClass.primaryConstructor: IrConstructor?
    get() = this.declarations.singleOrNull { it is IrConstructor && it.isPrimary } as IrConstructor?

val IrDeclarationContainer.properties: Sequence<IrProperty>
    get() = declarations.asSequence().filterIsInstance<IrProperty>()

val IrFunction.explicitParameters: List<IrValueParameter>
    get() = (listOfNotNull(dispatchReceiverParameter, extensionReceiverParameter) + valueParameters)

val IrBody.statements: List<IrStatement>
    get() = when (this) {
        is IrBlockBody -> statements
        is IrExpressionBody -> listOf(expression)
        is IrSyntheticBody -> error("Synthetic body contains no statements: $this")
        else -> error("Unknown subclass of IrBody: $this")
    }

val IrClass.defaultType: IrSimpleType
    get() = this.thisReceiver!!.type as IrSimpleType

val IrSimpleFunction.isSynthesized: Boolean get() = descriptor.kind == CallableMemberDescriptor.Kind.SYNTHESIZED

val IrDeclaration.isReal: Boolean get() = !isFakeOverride

val IrDeclaration.isFakeOverride: Boolean
    get() = when (this) {
        is IrSimpleFunction -> isFakeOverride
        is IrProperty -> isFakeOverride
        is IrField -> isFakeOverride
        else -> false
    }

fun IrClass.isSubclassOf(ancestor: IrClass): Boolean {

    val alreadyVisited = mutableSetOf<IrClass>()

    fun IrClass.hasAncestorInSuperTypes(): Boolean = when {
        this === ancestor -> true
        this in alreadyVisited -> false
        else -> {
            alreadyVisited.add(this)
            superTypes.mapNotNull { ((it as? IrSimpleType)?.classifier as? IrClassSymbol)?.owner }.any { it.hasAncestorInSuperTypes() }
        }
    }

    return this.hasAncestorInSuperTypes()
}

fun IrSimpleFunction.collectRealOverrides(): Set<IrSimpleFunction> {
    if (isReal) return setOf(this)

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

    return realOverrides
}

// This implementation is from kotlin-native
// TODO: use this implementation instead of any other
fun IrSimpleFunction.resolveFakeOverride(): IrSimpleFunction? {
    return collectRealOverrides().singleOrNull { it.modality != Modality.ABSTRACT }
}

fun IrSimpleFunction.isOrOverridesSynthesized(): Boolean {
    if (isSynthesized) return true

    if (isFakeOverride) return overriddenSymbols.all { it.owner.isOrOverridesSynthesized() }

    return false
}

fun IrSimpleFunction.findInterfaceImplementation(): IrSimpleFunction? {
    if (isReal) return null

    if (isOrOverridesSynthesized()) return null

    return resolveFakeOverride()?.run { if (parentAsClass.isInterface) this else null }
}

fun IrField.resolveFakeOverride(): IrField? {
    var toVisit = setOf(this)
    val nonOverridden = mutableSetOf<IrField>()
    while (toVisit.isNotEmpty()) {
        nonOverridden += toVisit.filter { it.overriddenSymbols.isEmpty() }
        toVisit = toVisit.flatMap { it.overriddenSymbols }.map { it.owner }.toSet()
    }
    return nonOverridden.singleOrNull()
}

val IrClass.isAnnotationClass get() = kind == ClassKind.ANNOTATION_CLASS
val IrClass.isEnumClass get() = kind == ClassKind.ENUM_CLASS
val IrClass.isEnumEntry get() = kind == ClassKind.ENUM_ENTRY
val IrClass.isInterface get() = kind == ClassKind.INTERFACE
val IrClass.isClass get() = kind == ClassKind.CLASS
val IrClass.isObject get() = kind == ClassKind.OBJECT
val IrClass.isAnonymousObject get() = isClass && name == SpecialNames.NO_NAME_PROVIDED
val IrDeclarationWithName.fqNameWhenAvailable: FqName?
    get() = when (val parent = parent) {
        is IrDeclarationWithName -> parent.fqNameWhenAvailable?.child(name)
        is IrPackageFragment -> parent.fqName.child(name)
        else -> null
    }

val IrDeclaration.parentAsClass: IrClass
    get() = parent as? IrClass ?: error("Parent of this declaration is not a class: ${render()}")

fun IrClass.isLocalClass(): Boolean {
    var current: IrDeclarationParent? = this
    while (current != null && current !is IrPackageFragment) {
        if (current is IrDeclarationWithVisibility && current.visibility == Visibilities.LOCAL)
            return true
        current = (current as? IrDeclaration)?.parent
    }

    return false
}

tailrec fun IrElement.getPackageFragment(): IrPackageFragment? {
    if (this is IrPackageFragment) return this
    val vParent = (this as? IrDeclaration)?.parent
    return when (vParent) {
        is IrPackageFragment -> vParent
        is IrClass -> vParent.getPackageFragment()
        else -> null
    }
}

fun IrAnnotationContainer.getAnnotation(name: FqName): IrConstructorCall? =
    annotations.find {
        it.symbol.owner.parentAsClass.descriptor.fqNameSafe == name
    }

fun IrAnnotationContainer.hasAnnotation(name: FqName) =
    annotations.any {
        it.symbol.owner.parentAsClass.descriptor.fqNameSafe == name
    }

fun IrAnnotationContainer.hasAnnotation(symbol: IrClassSymbol) =
    annotations.any {
        it.symbol.owner.parentAsClass.symbol == symbol
    }


val IrConstructor.constructedClassType get() = (parent as IrClass).thisReceiver?.type!!

fun IrFunction.isFakeOverriddenFromAny(): Boolean {
    if (origin != IrDeclarationOrigin.FAKE_OVERRIDE) {
        return (parent as? IrClass)?.thisReceiver?.type?.isAny() ?: false
    }

    return (this as IrSimpleFunction).overriddenSymbols.all { it.owner.isFakeOverriddenFromAny() }
}

fun IrCall.isSuperToAny() = superQualifierSymbol?.let { this.symbol.owner.isFakeOverriddenFromAny() } ?: false

fun IrDeclaration.isEffectivelyExternal(): Boolean {

    fun IrFunction.effectiveParentDeclaration(): IrDeclaration? =
        when (this) {
            is IrSimpleFunction -> correspondingProperty ?: parent as? IrDeclaration
            else -> parent as? IrDeclaration
        }

    val parent = parent
    return when (this) {
        is IrFunction -> isExternal || (effectiveParentDeclaration()?.isEffectivelyExternal() ?: false)
        is IrField -> isExternal || parent is IrDeclaration && parent.isEffectivelyExternal()
        is IrProperty -> isExternal || parent is IrDeclaration && parent.isEffectivelyExternal()
        is IrClass -> isExternal || parent is IrDeclaration && parent.isEffectivelyExternal()
        else -> false
    }
}

fun IrFunction.isExternalOrInheritedFromExternal(): Boolean {
    fun isExternalOrInheritedFromExternalImpl(f: IrSimpleFunction): Boolean =
        f.isEffectivelyExternal() || f.overriddenSymbols.any { isExternalOrInheritedFromExternalImpl(it.owner) }

    return isEffectivelyExternal() || (this is IrSimpleFunction && isExternalOrInheritedFromExternalImpl(this))
}

inline fun <reified T : IrDeclaration> IrDeclarationContainer.findDeclaration(predicate: (T) -> Boolean): T? =
    declarations.find { it is T && predicate(it) } as? T

@Suppress("UNCHECKED_CAST")
inline fun <reified T : IrDeclaration> IrDeclarationContainer.filterDeclarations(predicate: (T) -> Boolean): List<T> =
    declarations.filter { it is T && predicate(it) } as List<T>

fun IrValueParameter.hasDefaultValue(): Boolean = DFS.ifAny(
    listOf(this),
    { current -> (current.parent as? IrSimpleFunction)?.overriddenSymbols?.map { it.owner.valueParameters[current.index] } ?: listOf() },
    { current -> current.defaultValue != null }
)

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

// In presence of `IrBlock`s, return the expression that actually serves as the value (the last one).
tailrec fun IrExpression.removeBlocks(): IrExpression? = when (this) {
    is IrBlock -> (statements.last() as? IrExpression)?.removeBlocks()
    else -> this
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

/**
 * Create new call based on given [call] and [newFunction]
 * [receiversAsArguments]: optionally convert call with dispatch receiver to static call
 * [argumentsAsReceivers]: optionally convert static call to call with dispatch receiver
 */

fun irConstructorCall(
    call: IrFunctionAccessExpression,
    newFunction: IrConstructor,
    receiversAsArguments: Boolean = false,
    argumentsAsReceivers: Boolean = false
): IrConstructorCall =
    irConstructorCall(call, newFunction.symbol, receiversAsArguments, argumentsAsReceivers)

fun irConstructorCall(
    call: IrFunctionAccessExpression,
    newSymbol: IrConstructorSymbol,
    receiversAsArguments: Boolean = false,
    argumentsAsDispatchers: Boolean = false
): IrConstructorCall =
    call.run {
        IrConstructorCallImpl(
            startOffset,
            endOffset,
            type,
            newSymbol,
            newSymbol.descriptor,
            typeArgumentsCount,
            0,
            call.valueArgumentsCount,
            origin
        ).apply {
            copyTypeAndValueArgumentsFrom(
                call,
                receiversAsArguments,
                argumentsAsDispatchers
            )
        }
    }

fun irCall(
    call: IrFunctionAccessExpression,
    newFunction: IrFunction,
    receiversAsArguments: Boolean = false,
    argumentsAsReceivers: Boolean = false
): IrCall =
    irCall(
        call,
        newFunction.symbol,
        receiversAsArguments,
        argumentsAsReceivers
    )

fun irCall(
    call: IrFunctionAccessExpression,
    newSymbol: IrFunctionSymbol,
    receiversAsArguments: Boolean = false,
    argumentsAsReceivers: Boolean = false
): IrCall =
    call.run {
        IrCallImpl(
            startOffset,
            endOffset,
            type,
            newSymbol,
            newSymbol.descriptor,
            typeArgumentsCount,
            origin
        ).apply {
            copyTypeAndValueArgumentsFrom(
                call,
                receiversAsArguments,
                argumentsAsReceivers
            )
        }
    }

fun IrFunctionAccessExpression.copyTypeAndValueArgumentsFrom(
    src: IrFunctionAccessExpression,
    receiversAsArguments: Boolean = false,
    argumentsAsReceivers: Boolean = false
) = copyTypeAndValueArgumentsFrom(
    src,
    src.symbol.owner,
    symbol.owner,
    receiversAsArguments,
    argumentsAsReceivers
)

fun IrFunctionReference.copyTypeAndValueArgumentsFrom(
    src: IrFunctionReference,
    receiversAsArguments: Boolean = false,
    argumentsAsReceivers: Boolean = false
) = copyTypeAndValueArgumentsFrom(
    src,
    src.symbol.owner,
    symbol.owner,
    receiversAsArguments,
    argumentsAsReceivers
)

private fun IrMemberAccessExpression.copyTypeAndValueArgumentsFrom(
    src: IrMemberAccessExpression,
    srcFunction: IrFunction,
    destFunction: IrFunction,
    receiversAsArguments: Boolean = false,
    argumentsAsReceivers: Boolean = false
) {
    copyTypeArgumentsFrom(src)

    var destValueArgumentIndex = 0
    var srcValueArgumentIndex = 0

    when {
        receiversAsArguments && srcFunction.dispatchReceiverParameter != null -> {
            putValueArgument(destValueArgumentIndex++, src.dispatchReceiver)
        }
        argumentsAsReceivers && destFunction.dispatchReceiverParameter != null -> {
            dispatchReceiver = src.getValueArgument(srcValueArgumentIndex++)
        }
        else -> {
            dispatchReceiver = src.dispatchReceiver
        }
    }

    when {
        receiversAsArguments && srcFunction.extensionReceiverParameter != null -> {
            putValueArgument(destValueArgumentIndex++, src.extensionReceiver)
        }
        argumentsAsReceivers && destFunction.extensionReceiverParameter != null -> {
            extensionReceiver = src.getValueArgument(srcValueArgumentIndex++)
        }
        else -> {
            extensionReceiver = src.extensionReceiver
        }
    }

    while (srcValueArgumentIndex < src.valueArgumentsCount) {
        putValueArgument(destValueArgumentIndex++, src.getValueArgument(srcValueArgumentIndex++))
    }
}

val IrDeclaration.file: IrFile
    get() = parent.let {
        when (it) {
            is IrFile -> it
            is IrPackageFragment -> TODO("Unknown file")
            is IrDeclaration -> it.file
            else -> TODO("Unexpected declaration parent")
        }
    }

val IrFunction.allTypeParameters: List<IrTypeParameter>
    get() = if (this is IrConstructor)
        parentAsClass.typeParameters + typeParameters
    else
        typeParameters

fun IrMemberAccessExpression.getTypeSubstitutionMap(irFunction: IrFunction): Map<IrTypeParameterSymbol, IrType> =
    irFunction.allTypeParameters.withIndex().associate {
        it.value.symbol to getTypeArgument(it.index)!!
    }

val IrFunctionReference.typeSubstitutionMap: Map<IrTypeParameterSymbol, IrType>
    get() = getTypeSubstitutionMap(symbol.owner)

val IrFunctionAccessExpression.typeSubstitutionMap: Map<IrTypeParameterSymbol, IrType>
    get() = getTypeSubstitutionMap(symbol.owner)
