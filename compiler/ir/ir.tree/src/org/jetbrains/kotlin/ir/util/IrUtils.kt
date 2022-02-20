/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.utils.DFS
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

/**
 * Binds the arguments explicitly represented in the IR to the parameters of the accessed function.
 * The arguments are to be evaluated in the same order as they appear in the resulting list.
 */
@Suppress("unused") // used in kotlin-native
@ObsoleteDescriptorBasedAPI
fun IrMemberAccessExpression<*>.getArguments(): List<Pair<ParameterDescriptor, IrExpression>> {
    val res = mutableListOf<Pair<ParameterDescriptor, IrExpression>>()
    val descriptor = symbol.descriptor as CallableDescriptor

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
@ObsoleteDescriptorBasedAPI
@Suppress("unused") // Used in kotlin-native
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
fun IrMemberAccessExpression<*>.getArgumentsWithIr(): List<Pair<IrValueParameter, IrExpression>> {
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
@ObsoleteDescriptorBasedAPI
fun IrMemberAccessExpression<*>.addArguments(args: Map<ParameterDescriptor, IrExpression>) {
    val descriptor = symbol.descriptor as CallableDescriptor
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

@ObsoleteDescriptorBasedAPI
@Suppress("unused") // Used in kotlin-native
fun IrMemberAccessExpression<*>.addArguments(args: List<Pair<ParameterDescriptor, IrExpression>>) =
    this.addArguments(args.toMap())

fun IrExpression.isNullConst() = this is IrConst<*> && this.kind == IrConstKind.Null

fun IrExpression.isTrueConst() = this is IrConst<*> && this.kind == IrConstKind.Boolean && this.value == true

fun IrExpression.isFalseConst() = this is IrConst<*> && this.kind == IrConstKind.Boolean && this.value == false

fun IrExpression.isIntegerConst(value: Int) = this is IrConst<*> && this.kind == IrConstKind.Int && this.value == value

fun IrExpression.coerceToUnit(builtins: IrBuiltIns, typeSystem: IrTypeSystemContext): IrExpression {
    return coerceToUnitIfNeeded(type, builtins, typeSystem)
}

fun IrExpression.coerceToUnitIfNeeded(valueType: IrType, irBuiltIns: IrBuiltIns, typeSystem: IrTypeSystemContext): IrExpression {
    return if (valueType.isSubtypeOf(irBuiltIns.unitType, typeSystem))
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

fun IrFunctionAccessExpression.usesDefaultArguments(): Boolean =
    symbol.owner.valueParameters.any { this.getValueArgument(it.index) == null }

fun IrValueParameter.createStubDefaultValue(): IrExpressionBody =
    factory.createExpressionBody(
        IrErrorExpressionImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, "Stub expression for default value of $name")
    )

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

fun IrFunction.addExplicitParametersTo(parametersList: MutableList<IrValueParameter>) {
    parametersList.addIfNotNull(dispatchReceiverParameter)
    parametersList.addAll(valueParameters.take(contextReceiverParametersCount))
    parametersList.addIfNotNull(extensionReceiverParameter)
    parametersList.addAll(valueParameters.drop(contextReceiverParametersCount))
}

private fun Boolean.toInt(): Int = if (this) 1 else 0

val IrFunction.explicitParametersCount: Int
    get() = (dispatchReceiverParameter != null).toInt() + (extensionReceiverParameter != null).toInt() +
            valueParameters.size

val IrFunction.explicitParameters: List<IrValueParameter>
    get() = ArrayList<IrValueParameter>(explicitParametersCount).also {
        addExplicitParametersTo(it)
    }

val IrBody.statements: List<IrStatement>
    get() = when (this) {
        is IrBlockBody -> statements
        is IrExpressionBody -> listOf(expression)
        is IrSyntheticBody -> error("Synthetic body contains no statements: $this")
        else -> error("Unknown subclass of IrBody: $this")
    }

val IrClass.defaultType: IrSimpleType
    get() = this.thisReceiver!!.type as IrSimpleType

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

fun IrSimpleFunction.findInterfaceImplementation(): IrSimpleFunction? {
    if (isReal) return null

    return resolveFakeOverride()?.run { if (parentAsClass.isInterface) this else null }
}

val IrClass.isAnnotationClass get() = kind == ClassKind.ANNOTATION_CLASS
val IrClass.isEnumClass get() = kind == ClassKind.ENUM_CLASS
val IrClass.isEnumEntry get() = kind == ClassKind.ENUM_ENTRY
val IrClass.isInterface get() = kind == ClassKind.INTERFACE
val IrClass.isClass get() = kind == ClassKind.CLASS
val IrClass.isObject get() = kind == ClassKind.OBJECT
val IrClass.isAnonymousObject get() = isClass && name == SpecialNames.NO_NAME_PROVIDED
val IrClass.isNonCompanionObject: Boolean get() = isObject && !isCompanion
val IrDeclarationWithName.fqNameWhenAvailable: FqName?
    get() = when (val parent = parent) {
        is IrDeclarationWithName -> parent.fqNameWhenAvailable?.child(name)
        is IrPackageFragment -> parent.fqName.child(name)
        else -> null
    }

val IrDeclaration.parentAsClass: IrClass
    get() = parent as? IrClass
        ?: error("Parent of this declaration is not a class: ${render()}")

tailrec fun IrElement.getPackageFragment(): IrPackageFragment? {
    if (this is IrPackageFragment) return this
    return when (val parent = (this as? IrDeclaration)?.parent) {
        is IrPackageFragment -> parent
        else -> parent?.getPackageFragment()
    }
}

fun IrConstructorCall.isAnnotation(name: FqName) = symbol.owner.parentAsClass.fqNameWhenAvailable == name

fun IrAnnotationContainer.getAnnotation(name: FqName): IrConstructorCall? =
    annotations.find { it.isAnnotation(name) }

fun IrAnnotationContainer.hasAnnotation(name: FqName) =
    annotations.any {
        it.symbol.owner.parentAsClass.hasEqualFqName(name)
    }

fun IrAnnotationContainer.hasAnnotation(symbol: IrClassSymbol) =
    annotations.any {
        it.symbol.owner.parentAsClass.symbol == symbol
    }


val IrConstructor.constructedClassType get() = (parent as IrClass).thisReceiver?.type!!

fun IrFunction.isFakeOverriddenFromAny(): Boolean {
    val simpleFunction = this as? IrSimpleFunction ?: return false

    if (!simpleFunction.isFakeOverride) {
        return (parent as? IrClass)?.thisReceiver?.type?.isAny() ?: false
    }

    return simpleFunction.overriddenSymbols.all { it.owner.isFakeOverriddenFromAny() }
}

fun IrCall.isSuperToAny() = superQualifierSymbol?.let { this.symbol.owner.isFakeOverriddenFromAny() } ?: false


fun IrDeclaration.hasInterfaceParent() =
    parent.safeAs<IrClass>()?.isInterface == true


fun IrPossiblyExternalDeclaration.isEffectivelyExternal(): Boolean =
    this.isExternal

fun IrDeclaration.isEffectivelyExternal(): Boolean =
    this is IrPossiblyExternalDeclaration && this.isExternal

fun IrFunction.isExternalOrInheritedFromExternal(): Boolean {
    fun isExternalOrInheritedFromExternalImpl(f: IrSimpleFunction): Boolean =
        f.isEffectivelyExternal() || f.overriddenSymbols.any { isExternalOrInheritedFromExternalImpl(it.owner) }

    return isEffectivelyExternal() || (this is IrSimpleFunction && isExternalOrInheritedFromExternalImpl(this))
}

inline fun <reified T : IrDeclaration> IrDeclarationContainer.findDeclaration(predicate: (T) -> Boolean): T? =
    declarations.find { it is T && predicate(it) } as? T

fun IrValueParameter.hasDefaultValue(): Boolean = DFS.ifAny(
    listOf(this),
    { current -> (current.parent as? IrSimpleFunction)?.overriddenSymbols?.map { it.owner.valueParameters[current.index] } ?: listOf() },
    { current -> current.defaultValue != null }
)

fun ReferenceSymbolTable.referenceClassifier(classifier: ClassifierDescriptor): IrClassifierSymbol =
    when (classifier) {
        is TypeParameterDescriptor ->
            referenceTypeParameter(classifier)
        is ScriptDescriptor ->
            referenceScript(classifier)
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
 * Create new call based on given [call] and [newSymbol]
 * [receiversAsArguments]: optionally convert call with dispatch receiver to static call
 * [argumentsAsDispatchers]: optionally convert static call to call with dispatch receiver
 */
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
    newFunction: IrSimpleFunction,
    receiversAsArguments: Boolean = false,
    argumentsAsReceivers: Boolean = false,
    newSuperQualifierSymbol: IrClassSymbol? = null,
    newReturnType: IrType? = null
): IrCall =
    irCall(
        call,
        newFunction.symbol,
        receiversAsArguments,
        argumentsAsReceivers,
        newSuperQualifierSymbol,
        newReturnType
    )

fun irCall(
    call: IrFunctionAccessExpression,
    newSymbol: IrSimpleFunctionSymbol,
    receiversAsArguments: Boolean = false,
    argumentsAsReceivers: Boolean = false,
    newSuperQualifierSymbol: IrClassSymbol? = null,
    newReturnType: IrType? = null
): IrCall =
    call.run {
        IrCallImpl(
            startOffset,
            endOffset,
            newReturnType ?: type,
            newSymbol,
            typeArgumentsCount,
            valueArgumentsCount = newSymbol.owner.valueParameters.size,
            origin = origin,
            superQualifierSymbol = newSuperQualifierSymbol
        ).apply {
            copyTypeAndValueArgumentsFrom(
                call,
                receiversAsArguments,
                argumentsAsReceivers
            )
        }
    }

fun IrMemberAccessExpression<IrFunctionSymbol>.copyTypeAndValueArgumentsFrom(
    src: IrMemberAccessExpression<IrFunctionSymbol>,
    receiversAsArguments: Boolean = false,
    argumentsAsReceivers: Boolean = false
) {
    copyTypeArgumentsFrom(src)
    copyValueArgumentsFrom(src, symbol.owner, receiversAsArguments, argumentsAsReceivers)
}

fun IrMemberAccessExpression<IrFunctionSymbol>.copyValueArgumentsFrom(
    src: IrMemberAccessExpression<IrFunctionSymbol>,
    destFunction: IrFunction,
    receiversAsArguments: Boolean = false,
    argumentsAsReceivers: Boolean = false
) {
    var destValueArgumentIndex = 0
    var srcValueArgumentIndex = 0

    val srcFunction = src.symbol.owner

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

val IrDeclaration.fileOrNull: IrFile?
    get() = when (val parent = parent) {
        is IrFile -> parent
        is IrPackageFragment -> null
        is IrDeclaration -> parent.fileOrNull
        else -> TODO("Unexpected declaration parent")
    }

val IrDeclaration.file: IrFile
    get() = fileOrNull ?: TODO("Unknown file")

val IrDeclaration.parentClassOrNull: IrClass?
    get() = parent.let {
        when (it) {
            is IrClass -> it
            is IrDeclaration -> it.parentClassOrNull
            else -> null
        }
    }

val IrFunction.allTypeParameters: List<IrTypeParameter>
    get() = if (this is IrConstructor)
        parentAsClass.typeParameters + typeParameters
    else
        typeParameters


fun IrMemberAccessExpression<*>.getTypeSubstitutionMap(irFunction: IrFunction): Map<IrTypeParameterSymbol, IrType> {
    val typeParameters = irFunction.allTypeParameters

    val superQualifierSymbol = (this as? IrCallImpl)?.superQualifierSymbol

    val receiverType =
        if (superQualifierSymbol != null) superQualifierSymbol.defaultType as? IrSimpleType
        else dispatchReceiver?.type as? IrSimpleType

    val dispatchReceiverTypeArguments = receiverType?.arguments ?: emptyList()

    if (typeParameters.isEmpty() && dispatchReceiverTypeArguments.isEmpty()) {
        return emptyMap()
    }

    val result = mutableMapOf<IrTypeParameterSymbol, IrType>()
    if (dispatchReceiverTypeArguments.isNotEmpty()) {
        val parentTypeParameters =
            if (irFunction is IrConstructor) {
                val constructedClass = irFunction.parentAsClass
                if (!constructedClass.isInner && dispatchReceiver != null) {
                    throw AssertionError("Non-inner class constructor reference with dispatch receiver:\n${this.dump()}")
                }
                extractTypeParameters(constructedClass.parent as IrClass)
            } else {
                extractTypeParameters(irFunction.parentClassOrNull!!)
            }
        for ((index, typeParam) in parentTypeParameters.withIndex()) {
            dispatchReceiverTypeArguments[index].typeOrNull?.let {
                result[typeParam.symbol] = it
            }
        }
    }
    return typeParameters.withIndex().associateTo(result) {
        it.value.symbol to getTypeArgument(it.index)!!
    }
}

val IrFunctionReference.typeSubstitutionMap: Map<IrTypeParameterSymbol, IrType>
    get() = getTypeSubstitutionMap(symbol.owner)

val IrFunctionAccessExpression.typeSubstitutionMap: Map<IrTypeParameterSymbol, IrType>
    get() = getTypeSubstitutionMap(symbol.owner)

val IrDeclaration.isFileClass: Boolean
    get() =
        origin == IrDeclarationOrigin.FILE_CLASS ||
                origin == IrDeclarationOrigin.SYNTHETIC_FILE_CLASS ||
                origin == IrDeclarationOrigin.JVM_MULTIFILE_CLASS

fun IrDeclaration.isFromJava(): Boolean =
    origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB ||
            parent is IrDeclaration && (parent as IrDeclaration).isFromJava()

val IrValueDeclaration.isImmutable: Boolean
    get() = this is IrValueParameter || this is IrVariable && !isVar

val IrStatementOrigin?.isLambda: Boolean
    get() = this == IrStatementOrigin.LAMBDA || this == IrStatementOrigin.ANONYMOUS_FUNCTION

val IrFunction.originalFunction: IrFunction
    get() = (this as? IrAttributeContainer)?.attributeOwnerId as? IrFunction ?: this

val IrProperty.originalProperty: IrProperty
    get() = attributeOwnerId as? IrProperty ?: this

fun IrExpression.isTrivial() =
    this is IrConst<*> ||
            this is IrGetValue ||
            this is IrGetObjectValue ||
            this is IrErrorExpressionImpl

fun IrExpression.shallowCopy(): IrExpression =
    shallowCopyOrNull()
        ?: error("Not a copyable expression: ${render()}")

fun IrExpression.shallowCopyOrNull(): IrExpression? =
    when (this) {
        is IrConst<*> -> shallowCopy()
        is IrGetObjectValue ->
            IrGetObjectValueImpl(
                startOffset,
                endOffset,
                type,
                symbol
            )
        is IrGetValueImpl ->
            IrGetValueImpl(
                startOffset,
                endOffset,
                type,
                symbol,
                origin
            )
        is IrErrorExpressionImpl ->
            IrErrorExpressionImpl(
                startOffset,
                endOffset,
                type,
                description
            )
        else -> null
    }

internal fun <T> IrConst<T>.shallowCopy() = IrConstImpl(
    startOffset,
    endOffset,
    type,
    kind,
    value
)

val IrDeclarationParent.isFacadeClass: Boolean
    get() = this is IrClass &&
            (origin == IrDeclarationOrigin.JVM_MULTIFILE_CLASS ||
                    origin == IrDeclarationOrigin.FILE_CLASS ||
                    origin == IrDeclarationOrigin.SYNTHETIC_FILE_CLASS)