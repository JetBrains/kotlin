/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.declarations.buildReceiverParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildTypeParameter
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.linkage.partial.IrUnimplementedOverridesStrategy.ProcessAsFakeOverrides
import org.jetbrains.kotlin.ir.overrides.FakeOverrideBuilderStrategy
import org.jetbrains.kotlin.ir.overrides.IrOverridingUtil
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.IrPropertySymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.DFS
import org.jetbrains.kotlin.utils.addIfNotNull
import java.io.StringWriter

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
        val arg = getValueArgument((it.descriptor as ValueParameterDescriptor).index)
        if (arg != null) {
            res += (it.symbol to arg)
        }
    }

    return res
}

/**
 * Binds all arguments represented in the IR to the parameters of the accessed function.
 * The arguments are to be evaluated in the same order as they appear in the resulting list.
 */
fun IrMemberAccessExpression<*>.getAllArgumentsWithIr(): List<Pair<IrValueParameter, IrExpression?>> {
    val irFunction = when (this) {
        is IrFunctionAccessExpression -> this.symbol.owner
        is IrFunctionReference -> this.symbol.owner
        is IrPropertyReference -> {
            assert(this.field == null) { "Field should be null to use `getArgumentsWithIr` on IrPropertyReference: ${this.dump()}}" }
            this.getter!!.owner
        }
        else -> error(this)
    }

    return getAllArgumentsWithIr(irFunction)
}

/**
 * Binds all arguments represented in the IR to the parameters of the explicitly given function.
 * The arguments are to be evaluated in the same order as they appear in the resulting list.
 */
fun IrMemberAccessExpression<*>.getAllArgumentsWithIr(irFunction: IrFunction): List<Pair<IrValueParameter, IrExpression?>> {
    val res = mutableListOf<Pair<IrValueParameter, IrExpression?>>()

    dispatchReceiver?.let { arg ->
        irFunction.dispatchReceiverParameter?.let { parameter -> res += (parameter to arg) }
    }

    extensionReceiver?.let { arg ->
        irFunction.extensionReceiverParameter?.let { parameter -> res += (parameter to arg) }
    }

    irFunction.valueParameters.forEachIndexed { index, it ->
        res += it to getValueArgument(index)
    }

    return res
}

/**
 * Binds the arguments explicitly represented in the IR to the parameters of the accessed function.
 * The arguments are to be evaluated in the same order as they appear in the resulting list.
 */
@Suppress("UNCHECKED_CAST")
fun IrMemberAccessExpression<*>.getArgumentsWithIr(): List<Pair<IrValueParameter, IrExpression>> {
    return getAllArgumentsWithIr().filter { it.second != null } as List<Pair<IrValueParameter, IrExpression>>
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

fun IrExpression.implicitCastIfNeededTo(type: IrType) =
    if (type == this.type || this.type.isNothing())
        this
    else
        IrTypeOperatorCallImpl(startOffset, endOffset, type, IrTypeOperator.IMPLICIT_CAST, type, this)

fun IrFunctionAccessExpression.usesDefaultArguments(): Boolean =
    symbol.owner.valueParameters.any { this.getValueArgument(it.index) == null && (!it.isVararg || it.defaultValue != null) }

fun IrValueParameter.createStubDefaultValue(): IrExpressionBody =
    factory.createExpressionBody(
        IrErrorExpressionImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, "Stub expression for default value of $name")
    )

val IrProperty.isSimpleProperty: Boolean
    get() {
        val getterFun = getter
        val setterFun = setter
        return !isFakeOverride &&
                !isLateinit &&
                modality === Modality.FINAL &&
                (getterFun == null || getterFun.origin == IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR) &&
                (setterFun == null || setterFun.origin == IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR)
    }

val IrClass.functions: Sequence<IrSimpleFunction>
    get() = declarations.asSequence().filterIsInstance<IrSimpleFunction>()

val IrClass.superClass: IrClass?
    get() = superTypes
        .firstOrNull { !it.isInterface() && !it.isAny() }
        ?.classOrNull
        ?.owner

val IrClassSymbol.functions: Sequence<IrSimpleFunctionSymbol>
    get() = owner.functions.map { it.symbol }

val IrClass.constructors: Sequence<IrConstructor>
    get() = declarations.asSequence().filterIsInstance<IrConstructor>()

val IrClass.defaultConstructor: IrConstructor?
    get() = constructors.firstOrNull { ctor -> ctor.valueParameters.all { it.defaultValue != null } }

val IrClassSymbol.constructors: Sequence<IrConstructorSymbol>
    get() = owner.constructors.map { it.symbol }

val IrClass.fields: Sequence<IrField>
    get() = declarations.asSequence().filterIsInstance<IrField>()

val IrClassSymbol.fields: Sequence<IrFieldSymbol>
    get() = owner.fields.map { it.symbol }

val IrClass.primaryConstructor: IrConstructor?
    get() = this.declarations.singleOrNull { it is IrConstructor && it.isPrimary } as IrConstructor?

val IrClass.invokeFun: IrSimpleFunction?
    get() = declarations.filterIsInstance<IrSimpleFunction>().singleOrNull { it.name.asString() == "invoke" }

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

fun IrElement.getPackageFragment(): IrPackageFragment? =
    this as? IrPackageFragment ?: (this as? IrDeclaration)?.getPackageFragment()

tailrec fun IrDeclaration.getPackageFragment(): IrPackageFragment {
    val parent = this.parent
    return parent as? IrPackageFragment
        ?: (parent as IrDeclaration).getPackageFragment()
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
    (parent as? IrClass)?.isInterface == true

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

@ObsoleteDescriptorBasedAPI
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

@ObsoleteDescriptorBasedAPI
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

    while (srcValueArgumentIndex < src.symbol.owner.contextReceiverParametersCount) {
        putValueArgument(destValueArgumentIndex++, src.getValueArgument(srcValueArgumentIndex++))
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
    get() = getPackageFragment() as? IrFile

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

val IrExpression.isConstantLike: Boolean
    get() = this is IrConst<*> || this is IrGetSingletonValue
            || this is IrGetValue && this.symbol.owner.origin == IrDeclarationOrigin.INSTANCE_RECEIVER

fun IrExpression.shallowCopy(): IrExpression =
    shallowCopyOrNull()
        ?: error("Not a copyable expression: ${render()}")

fun IrExpression.shallowCopyOrNull(): IrExpression? =
    when (this) {
        is IrConst<*> -> shallowCopy()
        is IrGetEnumValue ->
            IrGetEnumValueImpl(
                startOffset,
                endOffset,
                type,
                symbol
            )
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

fun IrExpression.remapReceiver(oldReceiver: IrValueParameter?, newReceiver: IrValueParameter?): IrExpression = when (this) {
    is IrGetField ->
        IrGetFieldImpl(startOffset, endOffset, symbol, type, receiver?.remapReceiver(oldReceiver, newReceiver), origin, superQualifierSymbol)
    is IrGetValue ->
        IrGetValueImpl(startOffset, endOffset, type, newReceiver?.symbol.takeIf { symbol == oldReceiver?.symbol } ?: symbol, origin)
    is IrCall ->
        IrCallImpl(startOffset, endOffset, type, symbol, typeArgumentsCount, valueArgumentsCount, origin, superQualifierSymbol).also {
            it.dispatchReceiver = dispatchReceiver?.remapReceiver(oldReceiver, newReceiver)
            it.extensionReceiver = extensionReceiver?.remapReceiver(oldReceiver, newReceiver)
        }
    else -> shallowCopy()
}

val IrDeclarationParent.isFacadeClass: Boolean
    get() = this is IrClass &&
            (origin == IrDeclarationOrigin.JVM_MULTIFILE_CLASS ||
                    origin == IrDeclarationOrigin.FILE_CLASS ||
                    origin == IrDeclarationOrigin.SYNTHETIC_FILE_CLASS)

fun ir2string(ir: IrElement?): String = ir?.render() ?: ""

@Suppress("unused") // Used in kotlin-native
fun ir2stringWhole(ir: IrElement?): String {
    val strWriter = StringWriter()
    ir?.accept(DumpIrTreeVisitor(strWriter), "")
    return strWriter.toString()
}

fun IrClass.addSimpleDelegatingConstructor(
    superConstructor: IrConstructor,
    irBuiltIns: IrBuiltIns,
    isPrimary: Boolean = false,
    origin: IrDeclarationOrigin? = null
): IrConstructor =
    addConstructor {
        val klass = this@addSimpleDelegatingConstructor
        this.startOffset = klass.startOffset
        this.endOffset = klass.endOffset
        this.origin = origin ?: klass.origin
        this.visibility = superConstructor.visibility
        this.isPrimary = isPrimary
    }.also { constructor ->
        constructor.valueParameters = superConstructor.valueParameters.mapIndexed { index, parameter ->
            parameter.copyTo(constructor, index = index)
        }

        constructor.body = factory.createBlockBody(
            startOffset, endOffset,
            listOf(
                IrDelegatingConstructorCallImpl(
                    startOffset, endOffset, irBuiltIns.unitType,
                    superConstructor.symbol, 0,
                    superConstructor.valueParameters.size
                ).apply {
                    constructor.valueParameters.forEachIndexed { idx, parameter ->
                        putValueArgument(idx, IrGetValueImpl(startOffset, endOffset, parameter.type, parameter.symbol))
                    }
                },
                IrInstanceInitializerCallImpl(startOffset, endOffset, this.symbol, irBuiltIns.unitType)
            )
        )
    }

val IrCall.isSuspend get() = (symbol.owner as? IrSimpleFunction)?.isSuspend == true
val IrFunctionReference.isSuspend get() = (symbol.owner as? IrSimpleFunction)?.isSuspend == true

val IrFunction.isOverridable get() = this is IrSimpleFunction && this.isOverridable

val IrSimpleFunction.isOverridable: Boolean
    get() = visibility != DescriptorVisibilities.PRIVATE && modality != Modality.FINAL && (parent as? IrClass)?.isFinalClass != true

val IrFunction.isOverridableOrOverrides: Boolean get() = this is IrSimpleFunction && (isOverridable || overriddenSymbols.isNotEmpty())

val IrDeclaration.isMemberOfOpenClass: Boolean
    get() {
        val parentClass = this.parent as? IrClass ?: return false
        return !parentClass.isFinalClass
    }

val IrClass.isFinalClass: Boolean
    get() = modality == Modality.FINAL && kind != ClassKind.ENUM_CLASS

val IrTypeParametersContainer.classIfConstructor get() = if (this is IrConstructor) parentAsClass else this

fun IrValueParameter.copyTo(
    irFunction: IrFunction,
    origin: IrDeclarationOrigin = this.origin,
    index: Int = this.index,
    startOffset: Int = this.startOffset,
    endOffset: Int = this.endOffset,
    name: Name = this.name,
    remapTypeMap: Map<IrTypeParameter, IrTypeParameter> = mapOf(),
    type: IrType = this.type.remapTypeParameters(
        (parent as IrTypeParametersContainer).classIfConstructor,
        irFunction.classIfConstructor,
        remapTypeMap
    ),
    varargElementType: IrType? = this.varargElementType, // TODO: remapTypeParameters here as well
    defaultValue: IrExpressionBody? = this.defaultValue,
    isCrossinline: Boolean = this.isCrossinline,
    isNoinline: Boolean = this.isNoinline,
    isAssignable: Boolean = this.isAssignable
): IrValueParameter {
    val symbol = IrValueParameterSymbolImpl()
    val defaultValueCopy = defaultValue?.let { originalDefault ->
        factory.createExpressionBody(originalDefault.startOffset, originalDefault.endOffset) {
            expression = originalDefault.expression.deepCopyWithVariables().also {
                it.patchDeclarationParents(irFunction)
            }
        }
    }
    return factory.createValueParameter(
        startOffset, endOffset, origin, symbol,
        name, index, type, varargElementType, isCrossinline = isCrossinline,
        isNoinline = isNoinline, isHidden = false, isAssignable = isAssignable
    ).also {
        it.parent = irFunction
        it.defaultValue = defaultValueCopy
        it.copyAnnotationsFrom(this)
    }
}

fun IrTypeParameter.copyToWithoutSuperTypes(
    target: IrTypeParametersContainer,
    index: Int = this.index,
    origin: IrDeclarationOrigin = this.origin
): IrTypeParameter = buildTypeParameter(target) {
    updateFrom(this@copyToWithoutSuperTypes)
    this.name = this@copyToWithoutSuperTypes.name
    this.origin = origin
    this.index = index
}

fun IrFunction.copyReceiverParametersFrom(from: IrFunction, substitutionMap: Map<IrTypeParameterSymbol, IrType>) {
    dispatchReceiverParameter = from.dispatchReceiverParameter?.run {
        factory.createValueParameter(
            startOffset, endOffset, origin,
            IrValueParameterSymbolImpl(),
            name, index,
            type.substitute(substitutionMap),
            varargElementType?.substitute(substitutionMap),
            isCrossinline, isNoinline,
            isHidden, isAssignable
        ).also { parameter ->
            parameter.parent = this@copyReceiverParametersFrom
        }
    }
    extensionReceiverParameter = from.extensionReceiverParameter?.copyTo(this)
}

fun IrFunction.copyValueParametersFrom(from: IrFunction, substitutionMap: Map<IrTypeParameterSymbol, IrType>) {
    copyReceiverParametersFrom(from, substitutionMap)
    val shift = valueParameters.size
    valueParameters += from.valueParameters.map {
        it.copyTo(this, index = it.index + shift, type = it.type.substitute(substitutionMap))
    }
}

fun IrFunction.copyParameterDeclarationsFrom(from: IrFunction) {
    assert(typeParameters.isEmpty())
    copyTypeParametersFrom(from)
    copyValueParametersFrom(from)
}

fun IrFunction.copyValueParametersFrom(from: IrFunction) {
    copyValueParametersFrom(from, makeTypeParameterSubstitutionMap(from, this))
}

fun IrTypeParametersContainer.copyTypeParameters(
    srcTypeParameters: List<IrTypeParameter>,
    origin: IrDeclarationOrigin? = null,
    parameterMap: Map<IrTypeParameter, IrTypeParameter>? = null
): List<IrTypeParameter> {
    val shift = typeParameters.size
    val oldToNewParameterMap = parameterMap.orEmpty().toMutableMap()
    // Any type parameter can figure in a boundary type for any other parameter.
    // Therefore, we first copy the parameters themselves, then set up their supertypes.
    val newTypeParameters = srcTypeParameters.mapIndexed { i, sourceParameter ->
        sourceParameter.copyToWithoutSuperTypes(this, index = i + shift, origin = origin ?: sourceParameter.origin).also {
            oldToNewParameterMap[sourceParameter] = it
        }
    }
    typeParameters += newTypeParameters
    srcTypeParameters.zip(newTypeParameters).forEach { (srcParameter, dstParameter) ->
        dstParameter.copySuperTypesFrom(srcParameter, oldToNewParameterMap)
    }
    return newTypeParameters
}

fun IrTypeParametersContainer.copyTypeParametersFrom(
    source: IrTypeParametersContainer,
    origin: IrDeclarationOrigin? = null,
    parameterMap: Map<IrTypeParameter, IrTypeParameter>? = null
) = copyTypeParameters(source.typeParameters, origin, parameterMap)

private fun IrTypeParameter.copySuperTypesFrom(source: IrTypeParameter, srcToDstParameterMap: Map<IrTypeParameter, IrTypeParameter>) {
    val target = this
    val sourceParent = source.parent as IrTypeParametersContainer
    val targetParent = target.parent as IrTypeParametersContainer
    target.superTypes = source.superTypes.map {
        it.remapTypeParameters(sourceParent, targetParent, srcToDstParameterMap)
    }
}

fun IrAnnotationContainer.copyAnnotations(): List<IrConstructorCall> {
    return annotations.map { it.deepCopyWithSymbols(this as? IrDeclarationParent) }
}

fun IrAnnotationContainer.copyAnnotationsWhen(filter: IrConstructorCall.() -> Boolean): List<IrConstructorCall> {
    return annotations.mapNotNull { if (it.filter()) it.deepCopyWithSymbols(this as? IrDeclarationParent) else null }
}

fun IrMutableAnnotationContainer.copyAnnotationsFrom(source: IrAnnotationContainer) {
    annotations += source.copyAnnotations()
}

fun makeTypeParameterSubstitutionMap(
    original: IrTypeParametersContainer,
    transformed: IrTypeParametersContainer
): Map<IrTypeParameterSymbol, IrType> =
    original.typeParameters
        .map { it.symbol }
        .zip(transformed.typeParameters.map { it.defaultType })
        .toMap()


// Copy value parameters, dispatch receiver, and extension receiver from source to value parameters of this function.
// Type of dispatch receiver defaults to source's dispatch receiver. It is overridable in case the new function and the old one are used in
// different contexts and expect different type of dispatch receivers. The overriding type should be assign compatible to the old type.
fun IrFunction.copyValueParametersToStatic(
    source: IrFunction,
    origin: IrDeclarationOrigin,
    dispatchReceiverType: IrType? = source.dispatchReceiverParameter?.type,
    numValueParametersToCopy: Int = source.valueParameters.size
) {
    val target = this
    assert(target.valueParameters.isEmpty())

    var shift = 0
    source.dispatchReceiverParameter?.let { originalDispatchReceiver ->
        assert(dispatchReceiverType!!.isSubtypeOfClass(originalDispatchReceiver.type.classOrNull!!))
        val type = dispatchReceiverType.remapTypeParameters(
            (originalDispatchReceiver.parent as IrTypeParametersContainer).classIfConstructor,
            target.classIfConstructor
        )

        target.valueParameters += originalDispatchReceiver.copyTo(
            target,
            origin = originalDispatchReceiver.origin,
            index = shift++,
            type = type,
            name = Name.identifier("\$this")
        )
    }
    source.extensionReceiverParameter?.let { originalExtensionReceiver ->
        target.valueParameters += originalExtensionReceiver.copyTo(
            target,
            origin = originalExtensionReceiver.origin,
            index = shift++,
            name = Name.identifier("\$receiver")
        )
    }

    for (oldValueParameter in source.valueParameters) {
        if (oldValueParameter.index >= numValueParametersToCopy) break
        target.valueParameters += oldValueParameter.copyTo(
            target,
            origin = origin,
            index = oldValueParameter.index + shift
        )
    }
}

fun IrFunctionAccessExpression.passTypeArgumentsFrom(irFunction: IrTypeParametersContainer, offset: Int = 0) {
    irFunction.typeParameters.forEachIndexed { i, param ->
        putTypeArgument(i + offset, param.defaultType)
    }
}

/**
 * Perform a substitution of type parameters occuring in [this]. In order of
 * precedence, parameter `P` is substituted with...
 *
 *   1) `T`, if `srcToDstParameterMap.get(P) == T`
 *   2) `T`, if `source.typeParameters[i] == P` and
 *      `target.typeParameters[i] == T`
 *   3) `P`
 *
 *  If [srcToDstParameterMap] is total on the domain of type parameters in
 *  [this], this effectively performs a substitution according to that map.
 */
fun IrType.remapTypeParameters(
    source: IrTypeParametersContainer,
    target: IrTypeParametersContainer,
    srcToDstParameterMap: Map<IrTypeParameter, IrTypeParameter>? = null
): IrType =
    when (this) {
        is IrSimpleType -> {
            val classifier = classifier.owner
            when {
                classifier is IrTypeParameter -> {
                    val newClassifier =
                        srcToDstParameterMap?.get(classifier) ?: if (classifier.parent == source)
                            target.typeParameters[classifier.index]
                        else
                            classifier
                    IrSimpleTypeImpl(newClassifier.symbol, nullability, arguments, annotations)
                }

                classifier is IrClass ->
                    IrSimpleTypeImpl(
                        classifier.symbol,
                        nullability,
                        arguments.map {
                            when (it) {
                                is IrTypeProjection -> makeTypeProjection(
                                    it.type.remapTypeParameters(source, target, srcToDstParameterMap),
                                    it.variance
                                )
                                is IrStarProjection -> it
                            }
                        },
                        annotations
                    )

                else -> this
            }
        }
        else -> this
    }

/* Copied from K/N */
fun IrDeclarationContainer.addChild(declaration: IrDeclaration) {
    this.declarations += declaration
    declaration.setDeclarationsParent(this)
}

fun <T : IrElement> T.setDeclarationsParent(parent: IrDeclarationParent): T {
    accept(SetDeclarationsParentVisitor, parent)
    return this
}

object SetDeclarationsParentVisitor : IrElementVisitor<Unit, IrDeclarationParent> {
    override fun visitElement(element: IrElement, data: IrDeclarationParent) {
        if (element !is IrDeclarationParent) {
            element.acceptChildren(this, data)
        }
    }

    override fun visitDeclaration(declaration: IrDeclarationBase, data: IrDeclarationParent) {
        declaration.parent = data
        super.visitDeclaration(declaration, data)
    }
}


val IrFunction.isStatic: Boolean
    get() = parent is IrClass && dispatchReceiverParameter == null

val IrDeclaration.isTopLevel: Boolean
    get() {
        if (parent is IrPackageFragment) return true
        val parentClass = parent as? IrClass
        return parentClass?.isFileClass == true && parentClass.parent is IrPackageFragment
    }

fun IrClass.createImplicitParameterDeclarationWithWrappedDescriptor() {
    thisReceiver = buildReceiverParameter(this, IrDeclarationOrigin.INSTANCE_RECEIVER, symbol.typeWithParameters(typeParameters))
}

fun IrFactory.createSpecialAnnotationClass(fqn: FqName, parent: IrPackageFragment) =
    buildClass {
        kind = ClassKind.ANNOTATION_CLASS
        name = fqn.shortName()
    }.apply {
        createImplicitParameterDeclarationWithWrappedDescriptor()
        this.parent = parent
        addConstructor {
            isPrimary = true
        }
    }

@Suppress("UNCHECKED_CAST")
fun isElseBranch(branch: IrBranch) = branch is IrElseBranch || ((branch.condition as? IrConst<Boolean>)?.value == true)

fun IrFunction.isMethodOfAny(): Boolean =
    extensionReceiverParameter == null && dispatchReceiverParameter != null &&
            when (name) {
                OperatorNameConventions.HASH_CODE, OperatorNameConventions.TO_STRING -> valueParameters.isEmpty()
                OperatorNameConventions.EQUALS -> valueParameters.singleOrNull()?.type?.isNullableAny() == true
                else -> false
            }

fun IrDeclarationContainer.simpleFunctions() = declarations.flatMap {
    when (it) {
        is IrSimpleFunction -> listOf(it)
        is IrProperty -> listOfNotNull(it.getter, it.setter)
        else -> emptyList()
    }
}


fun IrClass.createParameterDeclarations() {
    assert(thisReceiver == null)
    thisReceiver = buildReceiverParameter(this, IrDeclarationOrigin.INSTANCE_RECEIVER, symbol.typeWithParameters(typeParameters))
}

fun IrFunction.createDispatchReceiverParameter(origin: IrDeclarationOrigin? = null) {
    assert(dispatchReceiverParameter == null)

    dispatchReceiverParameter = factory.createValueParameter(
        startOffset, endOffset,
        origin ?: parentAsClass.origin,
        IrValueParameterSymbolImpl(),
        SpecialNames.THIS,
        -1,
        parentAsClass.defaultType,
        null,
        isCrossinline = false,
        isNoinline = false,
        isHidden = false,
        isAssignable = false
    ).apply {
        parent = this@createDispatchReceiverParameter
    }
}

val IrFunction.allParameters: List<IrValueParameter>
    get() = if (this is IrConstructor) {
        ArrayList<IrValueParameter>(allParametersCount).also {
            it.add(
                this.constructedClass.thisReceiver
                    ?: error(this.render())
            )
            addExplicitParametersTo(it)
        }
    } else {
        explicitParameters
    }

val IrFunction.allParametersCount: Int
    get() = if (this is IrConstructor) explicitParametersCount + 1 else explicitParametersCount

// This is essentially the same as FakeOverrideBuilder,
// but it bypasses SymbolTable.
// TODO: merge it with FakeOverrideBuilder.
private class FakeOverrideBuilderForLowerings : FakeOverrideBuilderStrategy(
    friendModules = emptyMap(),
    unimplementedOverridesStrategy = ProcessAsFakeOverrides
) {
    override fun linkFunctionFakeOverride(function: IrFunctionWithLateBinding, manglerCompatibleMode: Boolean) {
        function.acquireSymbol(IrSimpleFunctionSymbolImpl())
    }

    override fun linkPropertyFakeOverride(property: IrPropertyWithLateBinding, manglerCompatibleMode: Boolean) {
        val propertySymbol = IrPropertySymbolImpl()
        property.getter?.let { it.correspondingPropertySymbol = propertySymbol }
        property.setter?.let { it.correspondingPropertySymbol = propertySymbol }

        property.acquireSymbol(propertySymbol)

        property.getter?.let {
            it.correspondingPropertySymbol = property.symbol
            linkFunctionFakeOverride(it as? IrFunctionWithLateBinding ?: error("Unexpected fake override getter: $it"), manglerCompatibleMode)
        }
        property.setter?.let {
            it.correspondingPropertySymbol = property.symbol
            linkFunctionFakeOverride(it as? IrFunctionWithLateBinding ?: error("Unexpected fake override setter: $it"), manglerCompatibleMode)
        }
    }
}

fun IrClass.addFakeOverrides(
    typeSystem: IrTypeSystemContext,
    implementedMembers: List<IrOverridableMember> = emptyList(),
    ignoredParentSymbols: List<IrSymbol> = emptyList()
) {
    IrOverridingUtil(typeSystem, FakeOverrideBuilderForLowerings())
        .buildFakeOverridesForClassUsingOverriddenSymbols(this,
                                                          implementedMembers = implementedMembers,
                                                          compatibilityMode = false,
                                                          ignoredParentSymbols = ignoredParentSymbols)
        .forEach { addChild(it) }
}

fun IrFactory.createStaticFunctionWithReceivers(
    irParent: IrDeclarationParent,
    name: Name,
    oldFunction: IrFunction,
    dispatchReceiverType: IrType? = oldFunction.dispatchReceiverParameter?.type,
    origin: IrDeclarationOrigin = oldFunction.origin,
    modality: Modality = Modality.FINAL,
    visibility: DescriptorVisibility = oldFunction.visibility,
    isFakeOverride: Boolean = oldFunction.isFakeOverride,
    copyMetadata: Boolean = true,
    typeParametersFromContext: List<IrTypeParameter> = listOf()
): IrSimpleFunction {
    return createFunction(
        oldFunction.startOffset, oldFunction.endOffset,
        origin,
        IrSimpleFunctionSymbolImpl(),
        name,
        visibility,
        modality,
        oldFunction.returnType,
        isInline = oldFunction.isInline,
        isExternal = false,
        isTailrec = false,
        isSuspend = oldFunction.isSuspend,
        isExpect = oldFunction.isExpect,
        isFakeOverride = isFakeOverride,
        isOperator = oldFunction is IrSimpleFunction && oldFunction.isOperator,
        isInfix = oldFunction is IrSimpleFunction && oldFunction.isInfix,
        containerSource = oldFunction.containerSource,
    ).apply {
        parent = irParent

        val newTypeParametersFromContext = copyAndRenameConflictingTypeParametersFrom(
            typeParametersFromContext,
            oldFunction.typeParameters
        )
        val newTypeParametersFromFunction = copyTypeParametersFrom(oldFunction)
        val typeParameterMap =
            (typeParametersFromContext + oldFunction.typeParameters)
                .zip(newTypeParametersFromContext + newTypeParametersFromFunction).toMap()

        fun remap(type: IrType): IrType =
            type.remapTypeParameters(oldFunction, this, typeParameterMap)

        typeParameters.forEach { it.superTypes = it.superTypes.map(::remap) }

        annotations = oldFunction.annotations

        valueParameters = buildList {
            var offset = 0

            addIfNotNull(
                oldFunction.dispatchReceiverParameter?.copyTo(
                    this@apply,
                    name = Name.identifier("\$this"),
                    index = offset++,
                    type = remap(dispatchReceiverType!!),
                    origin = IrDeclarationOrigin.MOVED_DISPATCH_RECEIVER
                )
            )

            addAll(
                oldFunction.valueParameters
                    .asSequence()
                    .take(oldFunction.contextReceiverParametersCount)
                    .map {
                        it.copyTo(
                            this@apply,
                            index = offset++,
                            remapTypeMap = typeParameterMap
                        )
                    }
            )

            addIfNotNull(
                oldFunction.extensionReceiverParameter?.copyTo(
                    this@apply,
                    name = Name.identifier("\$receiver"),
                    index = offset++,
                    origin = IrDeclarationOrigin.MOVED_EXTENSION_RECEIVER,
                    remapTypeMap = typeParameterMap
                )
            )

            addAll(
                oldFunction.valueParameters
                    .asSequence()
                    .drop(oldFunction.contextReceiverParametersCount)
                    .map {
                        it.copyTo(
                            this@apply,
                            index = offset++,
                            remapTypeMap = typeParameterMap
                        )
                    }
            )
        }

        if (copyMetadata) metadata = oldFunction.metadata

        copyAttributes(oldFunction as? IrAttributeContainer)
    }
}

/**
 * Appends the parameters in [contextParameters] to the type parameters of
 * [this] function, renaming those that may clash with a provided collection of
 * [existingParameters] (e.g. type parameters of the function itself, when
 * creating DefaultImpls).
 *
 * @returns List of newly created, possibly renamed, copies of type parameters
 *     in order of the corresponding parameters in [context].
 */
private fun IrSimpleFunction.copyAndRenameConflictingTypeParametersFrom(
    contextParameters: List<IrTypeParameter>,
    existingParameters: Collection<IrTypeParameter>
): List<IrTypeParameter> {
    val newParameters = mutableListOf<IrTypeParameter>()

    val existingNames =
        (contextParameters.map { it.name.asString() } + existingParameters.map { it.name.asString() }).toMutableSet()

    contextParameters.forEachIndexed { i, contextType ->
        val newName = if (existingParameters.any { it.name.asString() == contextType.name.asString() }) {
            val newNamePrefix = contextType.name.asString() + "_I"
            val newName = newNamePrefix + generateSequence(1) { x -> x + 1 }.first { n ->
                (newNamePrefix + n) !in existingNames
            }
            existingNames.add(newName)
            newName
        } else {
            contextType.name.asString()
        }

        newParameters.add(buildTypeParameter(this) {
            updateFrom(contextType)
            index = i
            name = Name.identifier(newName)
        })
    }

    val zipped = contextParameters.zip(newParameters)
    val parameterMap = zipped.toMap()
    for ((oldParameter, newParameter) in zipped) {
        newParameter.copySuperTypesFrom(oldParameter, parameterMap)
    }

    typeParameters = typeParameters + newParameters

    return newParameters
}

val IrSymbol.isSuspend: Boolean
    get() = this is IrSimpleFunctionSymbol && owner.isSuspend

fun <T : IrOverridableDeclaration<*>> T.allOverridden(includeSelf: Boolean = false): List<T> {
    val result = mutableListOf<T>()
    if (includeSelf) {
        result.add(this)
    }

    var current = this
    while (true) {
        val overridden = current.overriddenSymbols
        when (overridden.size) {
            0 -> return result
            1 -> {
                @Suppress("UNCHECKED_CAST")
                current = overridden[0].owner as T
                result.add(current)
            }
            else -> {
                val resultSet = result.toMutableSet()
                computeAllOverridden(current, resultSet)
                return resultSet.toList()
            }
        }
    }
}

private fun <T : IrOverridableDeclaration<*>> computeAllOverridden(overridable: T, result: MutableSet<T>) {
    for (overriddenSymbol in overridable.overriddenSymbols) {
        @Suppress("UNCHECKED_CAST") val override = overriddenSymbol.owner as T
        if (result.add(override)) {
            computeAllOverridden(override, result)
        }
    }
}

fun IrBuiltIns.getKFunctionType(returnType: IrType, parameterTypes: List<IrType>) =
    kFunctionN(parameterTypes.size).typeWith(parameterTypes + returnType)

fun IdSignature?.isComposite(): Boolean =
    this is IdSignature.CompositeSignature

fun IrFunction.isToString(): Boolean =
    name == OperatorNameConventions.TO_STRING && extensionReceiverParameter == null && contextReceiverParametersCount == 0 && valueParameters.isEmpty()

fun IrFunction.isHashCode() =
    name == OperatorNameConventions.HASH_CODE && extensionReceiverParameter == null && contextReceiverParametersCount == 0 && valueParameters.isEmpty()

fun IrFunction.isEquals() =
    name == OperatorNameConventions.EQUALS &&
            extensionReceiverParameter == null && contextReceiverParametersCount == 0 &&
            valueParameters.singleOrNull()?.type?.isNullableAny() == true

val IrFunction.isValueClassTypedEquals: Boolean
    get() {
        val parentClass = parent as? IrClass ?: return false
        val enclosingClassStartProjection = parentClass.symbol.starProjectedType
        return name == OperatorNameConventions.EQUALS
                && (returnType.isBoolean() || returnType.isNothing())
                && valueParameters.size == 1
                && (valueParameters[0].type == enclosingClassStartProjection)
                && contextReceiverParametersCount == 0 && extensionReceiverParameter == null
                && (parentClass.isValue)
    }

/**
 * The method is used to calculate the previous offset from the current one to prevent situations when it can calculate
 * [UNDEFINED_OFFSET] from 0 offset and [SYNTHETIC_OFFSET] offset from the [UNDEFINED OFFSET]
 */
val Int.previousOffset
    get(): Int =
        when (this) {
            0 -> 0
            UNDEFINED_OFFSET -> UNDEFINED_OFFSET
            SYNTHETIC_OFFSET -> SYNTHETIC_OFFSET
            else -> if (this > 0) minus(1) else error("Invalid offset appear")
        }

fun IrAttributeContainer.extractRelatedDeclaration(): IrDeclaration? {
    return when (this) {
        is IrClass -> this
        is IrFunctionExpression -> function
        is IrFunctionReference -> symbol.owner
        else -> null
    }
}