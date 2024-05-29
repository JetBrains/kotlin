/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.util

import org.jetbrains.kotlin.bir.*
import org.jetbrains.kotlin.bir.declarations.*
import org.jetbrains.kotlin.bir.declarations.impl.BirTypeParameterImpl
import org.jetbrains.kotlin.bir.declarations.impl.BirValueParameterImpl
import org.jetbrains.kotlin.bir.expressions.*
import org.jetbrains.kotlin.bir.symbols.*
import org.jetbrains.kotlin.bir.types.*
import org.jetbrains.kotlin.bir.types.utils.defaultType
import org.jetbrains.kotlin.bir.types.utils.isNullable
import org.jetbrains.kotlin.bir.types.utils.substitute
import org.jetbrains.kotlin.bir.types.utils.typeOrNull
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.InlineClassRepresentation
import org.jetbrains.kotlin.descriptors.MultiFieldValueClassRepresentation
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.utils.*
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import java.io.File

val BirDeclaration.parentAsClass: BirClass
    get() = parent as? BirClass
        ?: error("Parent of this declaration is not a class: ${render()}")


fun BirClass.companionObject(): BirClass? =
    this.declarations.singleOrNull { it is BirClass && it.isCompanion } as BirClass?

val BirDeclaration.isGetter
    get() = this is BirSimpleFunction && this == this.correspondingPropertySymbol?.ownerIfBound?.getter

val BirDeclaration.isSetter
    get() = this is BirSimpleFunction && this == this.correspondingPropertySymbol?.ownerIfBound?.setter

val BirDeclaration.isAccessor
    get() = this.isGetter || this.isSetter

val BirDeclaration.isPropertyAccessor
    get() =
        this is BirSimpleFunction && this.correspondingPropertySymbol != null

val BirDeclaration.isPropertyField
    get() =
        this is BirField && this.correspondingPropertySymbol != null

val BirDeclaration.isAnonymousObject get() = this is BirClass && name == SpecialNames.NO_NAME_PROVIDED

val BirDeclaration.isAnonymousFunction get() = this is BirSimpleFunction && name == SpecialNames.NO_NAME_PROVIDED

val BirFunction.isStatic: Boolean
    get() = parent is BirClass && dispatchReceiverParameter == null


val BirClass.functions: Sequence<BirSimpleFunction>
    get() = declarations.asSequence().filterIsInstance<BirSimpleFunction>()

val BirClass.constructors: Sequence<BirConstructor>
    get() = declarations.asSequence().filterIsInstance<BirConstructor>()

val BirClass.defaultConstructor: BirConstructor?
    get() = constructors.firstOrNull { ctor -> ctor.valueParameters.all { it.defaultValue != null } }

val BirClass.fields: Sequence<BirField>
    get() = declarations.asSequence().filterIsInstance<BirField>()

val BirClass.primaryConstructor: BirConstructor?
    get() = this.declarations.singleOrNull { it is BirConstructor && it.isPrimary } as BirConstructor?


val BirClass.isSingleFieldValueClass: Boolean
    get() = valueClassRepresentation is InlineClassRepresentation

val BirClass.isMultiFieldValueClass: Boolean
    get() = valueClassRepresentation is MultiFieldValueClassRepresentation

val BirClass.inlineClassRepresentation: InlineClassRepresentation<BirSimpleType>?
    get() = valueClassRepresentation as? InlineClassRepresentation<BirSimpleType>

val BirClass.multiFieldValueClassRepresentation: MultiFieldValueClassRepresentation<BirSimpleType>?
    get() = valueClassRepresentation as? MultiFieldValueClassRepresentation<BirSimpleType>


fun BirClass.getProperty(name: String): BirProperty? {
    val properties = declarations.filterIsInstanceAnd<BirProperty> { it.name.asString() == name }
    if (properties.size > 1) error(properties)
    return properties.singleOrNull()
}

fun BirClass.getSimpleFunction(name: String): BirSimpleFunction? =
    findDeclaration<BirSimpleFunction> { it.name.asString() == name }

fun BirClass.getPropertyGetter(name: String): BirSimpleFunction? =
    getProperty(name)?.getter
        ?: getSimpleFunction("<get-$name>").also { assert(it?.correspondingPropertySymbol?.ownerIfBound?.name?.asString() == name) }

fun BirClass.getPropertySetter(name: String): BirSimpleFunction? =
    getProperty(name)?.setter
        ?: getSimpleFunction("<set-$name>").also { assert(it?.correspondingPropertySymbol?.ownerIfBound?.name?.asString() == name) }


inline fun <reified T : BirDeclaration> BirDeclarationContainer.findDeclaration(predicate: (T) -> Boolean): T? =
    declarations.find { it is T && predicate(it) } as? T


val BirClass.defaultType: BirSimpleType
    get() = thisReceiver!!.type as BirSimpleType

val BirConstructor.constructedClass
    get() = this.parent as BirClass

val BirConstructorCall.constructedClass
    get() = this.symbol.owner.constructedClass

fun BirConstructorCall.isAnnotationWithEqualFqName(fqName: FqName): Boolean =
    constructedClass.hasEqualFqName(fqName)

val BirClass.packageFqName: FqName?
    get() = signature?.packageFqName() ?: ancestors().firstNotNullOfOrNull { (it as? BirPackageFragment)?.packageFqName }

fun BirDeclarationWithName.hasEqualFqName(fqName: FqName): Boolean {
    if ((this as BirSymbol).hasEqualFqName(fqName)) {
        return true
    }
    if (name != fqName.shortName()) {
        return false
    }

    ancestors().forEach {
        when (it) {
            is BirPackageFragment -> return it.packageFqName == fqName.parent()
            is BirDeclarationWithName -> return it.hasEqualFqName(fqName.parent())
        }
    }

    return false
}

fun BirSymbol.hasEqualFqName(fqName: FqName): Boolean {
    return /*todo: is public && */ with(signature as? IdSignature.CommonSignature ?: return false) {
        FqName("$packageFqName.$declarationFqName") == fqName
    }
}

@Suppress("RecursivePropertyAccessor")
val BirDeclarationWithName.fqNameWhenAvailable: FqName?
    get() = ancestors().firstNotNullOfOrNull {
        when (it) {
            is BirDeclarationWithName -> it.fqNameWhenAvailable?.child(name)
            is BirPackageFragment -> it.packageFqName.child(name)
            else -> null
        }
    }

@Suppress("RecursivePropertyAccessor")
val BirClass.classId: ClassId?
    get() = ancestors().firstNotNullOfOrNull {
        when (it) {
            is BirClass -> it.classId?.createNestedClassId(this.name)
            is BirPackageFragment -> ClassId.topLevel(it.packageFqName.child(this.name))
            else -> null
        }
    }


val BirConstructorCall.isAnnotation get() = symbol.owner.parentAsClass.kind == ClassKind.ANNOTATION_CLASS
fun BirConstructorCall.isAnnotation(name: FqName) = symbol.owner.parentAsClass.fqNameWhenAvailable == name
fun BirConstructorCall.isAnnotation(annotationClass: BirClassSymbol) = symbol.owner.parentAsClass == annotationClass

fun BirAnnotationContainer.getAnnotation(name: FqName): BirConstructorCall? =
    annotations.find { it.isAnnotation(name) }

fun BirAnnotationContainer.getAnnotation(annotationClass: BirClassSymbol): BirConstructorCall? =
    annotations.find { it.isAnnotation(annotationClass) }

fun BirAnnotationContainer.hasAnnotation(name: FqName) =
    getAnnotation(name) != null

fun BirAnnotationContainer.hasAnnotation(annotationClass: BirClassSymbol) =
    getAnnotation(annotationClass) != null

fun BirValueParameter.getIndex(): Int {
    val list = (this as BirElementBase).getContainingList()
        ?: return -1
    return list.indexOf(this)
}

fun BirTypeParameter.getIndex(): Int {
    val list = getContainingList()
        ?: return -1
    return list.indexOf(this)
}

fun makeTypeParameterSubstitutionMap(
    original: BirTypeParametersContainer,
    transformed: BirTypeParametersContainer,
): Map<BirTypeParameterSymbol, BirType> =
    original.typeParameters.map { it.symbol }
        .zip(transformed.typeParameters.map { it.defaultType })
        .toMap()

@OptIn(ObsoleteDescriptorBasedAPI::class)
fun BirFunction.copyReceiverParametersFrom(from: BirFunction, substitutionMap: Map<BirTypeParameterSymbol, BirType>) {
    dispatchReceiverParameter = from.dispatchReceiverParameter?.run {
        BirValueParameterImpl(
            sourceSpan = sourceSpan,
            signature = signature,
            annotations = emptyList(),
            origin = origin,
            name = name,
            type = type.substitute(substitutionMap),
            isAssignable = isAssignable,
            index = index,
            varargElementType = varargElementType?.substitute(substitutionMap),
            isCrossinline = isCrossinline,
            isNoinline = isNoinline,
            isHidden = isHidden,
            defaultValue = null,
        )
    }
    extensionReceiverParameter = from.extensionReceiverParameter?.copyTo(this)
}

val BirTypeParametersContainer.classIfConstructor get() = if (this is BirConstructor) parentAsClass else this

@OptIn(ObsoleteDescriptorBasedAPI::class)
fun BirValueParameter.copyTo(
    targetFunction: BirFunction,
    index: Int = this.index,
    remapTypeMap: Map<BirTypeParameter, BirTypeParameter> = emptyMap(),
    type: BirType = this.type.remapTypeParameters(
        (parent as BirTypeParametersContainer).classIfConstructor,
        targetFunction.classIfConstructor,
        remapTypeMap
    ),
    defaultValue: BirExpressionBody? = this.defaultValue,
): BirValueParameter {
    val defaultValueCopy = defaultValue?.deepCopy()
    return BirValueParameterImpl(
        sourceSpan = sourceSpan,
        signature = signature,
        annotations = emptyList(),
        origin = origin,
        name = name,
        type = type,
        isAssignable = isAssignable,
        index = index,
        varargElementType = varargElementType,
        isCrossinline = isCrossinline,
        isNoinline = isNoinline,
        isHidden = false,
        defaultValue = null,
    ).apply {
        this.defaultValue = defaultValueCopy
        annotations += copyAnnotations()
    }
}

/*fun BirAnnotationContainer.copyAnnotationsFrom(source: BirAnnotationContainer) {
    annotations = annotations memoryOptimizedPlus source.copyAnnotations()
}*/

fun BirAnnotationContainer.copyAnnotations(): List<BirConstructorCall> {
    return annotations.memoryOptimizedMap { it.deepCopy() }
}

fun BirFunction.copyValueParametersFrom(from: BirFunction, substitutionMap: Map<BirTypeParameterSymbol, BirType>) {
    copyReceiverParametersFrom(from, substitutionMap)
    valueParameters += from.valueParameters.map {
        it.copyTo(this, type = it.type.substitute(substitutionMap))
    }
}

fun BirFunction.copyValueParametersFrom(from: BirFunction) {
    copyValueParametersFrom(from, makeTypeParameterSubstitutionMap(from, this))
}

fun BirTypeParametersContainer.copyTypeParameters(
    srcTypeParameters: Collection<BirTypeParameter>,
    origin: IrDeclarationOrigin? = null,
    parameterMap: Map<BirTypeParameter, BirTypeParameter>? = null,
): List<BirTypeParameter> {
    val shift = typeParameters.size
    val oldToNewParameterMap = parameterMap.orEmpty().toMutableMap()
    // Any type parameter can figure in a boundary type for any other parameter.
    // Therefore, we first copy the parameters themselves, then set up their supertypes.
    val newTypeParameters = srcTypeParameters.memoryOptimizedMapIndexed { i, sourceParameter ->
        sourceParameter.copyWithoutSuperTypes(i + shift, origin ?: sourceParameter.origin).also {
            oldToNewParameterMap[sourceParameter] = it
        }
    }
    typeParameters += newTypeParameters
    srcTypeParameters.zip(newTypeParameters).forEach { (srcParameter, dstParameter) ->
        dstParameter.copySuperTypesFrom(srcParameter, oldToNewParameterMap)
    }
    return newTypeParameters
}

fun BirTypeParametersContainer.copyTypeParametersFrom(
    source: BirTypeParametersContainer,
    origin: IrDeclarationOrigin? = null,
    parameterMap: Map<BirTypeParameter, BirTypeParameter>? = null,
) = copyTypeParameters(source.typeParameters, origin, parameterMap)

private fun BirTypeParameter.copySuperTypesFrom(source: BirTypeParameter, srcToDstParameterMap: Map<BirTypeParameter, BirTypeParameter>) {
    val symbol = this
    val sourceParent = source.parent as BirTypeParametersContainer
    val targetParent = symbol.parent as BirTypeParametersContainer
    symbol.superTypes = source.superTypes.memoryOptimizedMap {
        it.remapTypeParameters(sourceParent, targetParent, srcToDstParameterMap)
    }
}

@OptIn(ObsoleteDescriptorBasedAPI::class)
fun BirTypeParameter.copyWithoutSuperTypes(
    index: Int = this.index,
    origin: IrDeclarationOrigin = this.origin,
): BirTypeParameter = BirTypeParameterImpl(
    sourceSpan = sourceSpan,
    origin = origin,
    name = name,
    variance = variance,
    isReified = isReified,
    index = index,
    signature = signature,
    annotations = emptyList(),
    superTypes = emptyList(),
)

/**
 * Perform a substitution of type parameters occurring in [this]. In order of
 * precedence, parameter `P` is substituted with...
 *
 *   1) `T`, if `srcToDstParameterMap.get(P) == T`
 *   2) `T`, if `source.typeParameters[i] == P` and
 *      `symbol.typeParameters[i] == T`
 *   3) `P`
 *
 *  If [srcToDstParameterMap] is total on the domain of type parameters in
 *  [this], this effectively performs a substitution according to that map.
 */
fun BirType.remapTypeParameters(
    source: BirTypeParametersContainer,
    symbol: BirTypeParametersContainer,
    srcToDstParameterMap: Map<BirTypeParameter, BirTypeParameter>? = null,
): BirType = when (this) {
    is BirSimpleType -> {
        when (val classifier = classifier) {
            is BirTypeParameter -> {
                val newClassifier = srcToDstParameterMap?.get(classifier)
                    ?: if (classifier.parent == source)
                        symbol.typeParameters.elementAt(classifier.getIndex())
                    else
                        classifier
                BirSimpleTypeImpl(newClassifier.symbol, nullability, arguments, annotations)
            }
            is BirClass -> BirSimpleTypeImpl(
                classifier.symbol,
                nullability,
                arguments.memoryOptimizedMap {
                    when (it) {
                        is BirTypeProjection -> makeTypeProjection(
                            it.type.remapTypeParameters(source, symbol, srcToDstParameterMap),
                            it.variance
                        )
                        is BirStarProjection -> it
                    }
                },
                annotations
            )
            else -> this
        }
    }
    else -> this
}

val BirDeclaration.isFileClass: Boolean
    get() = origin == IrDeclarationOrigin.FILE_CLASS ||
            origin == IrDeclarationOrigin.SYNTHETIC_FILE_CLASS ||
            origin == IrDeclarationOrigin.JVM_MULTIFILE_CLASS

val BirDeclaration.isTopLevel: Boolean
    get() {
        if (parent is BirPackageFragment) return true
        val parentClass = parent as? BirClass
        return parentClass?.isFileClass == true && parentClass.parent is BirPackageFragment
    }

fun BirValueParameter.isInlineParameter(type: BirType = this.type) =
    index >= 0 && !isNoinline && (type.isFunction() || type.isSuspendFunction()) &&
            // Parameters with default values are always nullable, so check the expression too.
            // Note that the frontend has a diagnostic for nullable inline parameters, so actually
            // making this return `false` requires using `@Suppress`.
            (!type.isNullable() || defaultValue?.expression?.type?.isNullable() == false)

val BirConstructorCall.classTypeArgumentsCount: Int
    get() = typeArguments.size - constructorTypeArgumentsCount

val BirFile.path: String get() = fileEntry.name
val BirFile.name: String get() = File(path).name


fun BirExpression.isAdaptedFunctionReference() =
    this is BirBlock && this.origin == IrStatementOrigin.ADAPTED_FUNCTION_REFERENCE

val BirDeclaration.isLocal: Boolean
    get() {
        return ancestors().any {
            it is BirDeclarationWithVisibility && it.visibility == DescriptorVisibilities.LOCAL
                    || it is BirDeclaration && it.isAnonymousObject
                    || it is BirScript
                    || it is BirClass && it.origin == IrDeclarationOrigin.SCRIPT_CLASS
        }
    }

fun BirInlinedFunctionBlock.isFunctionInlining(): Boolean {
    return this.inlinedElement is BirFunction
}

fun BirInlinedFunctionBlock.isLambdaInlining(): Boolean {
    return !isFunctionInlining()
}

fun BirStatement.isPartialLinkageRuntimeError(): Boolean {
    return when (this) {
        is BirCall -> origin == IrStatementOrigin.PARTIAL_LINKAGE_RUNTIME_ERROR //|| symbol == builtIns.linkageErrorSymbol
        is BirContainerExpression -> origin == IrStatementOrigin.PARTIAL_LINKAGE_RUNTIME_ERROR || statements.any { it.isPartialLinkageRuntimeError() }
        else -> false
    }
}

fun BirAttributeContainer.copyAttributes(
    other: BirAttributeContainer,
) {
    attributeOwnerId = other.attributeOwnerId
    this[GlobalBirDynamicProperties.OriginalBeforeInline] = other[GlobalBirDynamicProperties.OriginalBeforeInline]
}

val BirFunction.allTypeParameters: Collection<BirTypeParameter>
    get() = if (this is BirConstructor)
        parentAsClass.typeParameters + typeParameters
    else
        typeParameters


fun BirMemberAccessExpression<*>.getTypeSubstitutionMap(function: BirFunction): Map<BirTypeParameterSymbol, BirType> {
    val typeParameters = function.allTypeParameters

    val superQualifier = (this as? BirCall)?.superQualifierSymbol

    val receiverType =
        if (superQualifier != null) superQualifier.defaultType as? BirSimpleType
        else dispatchReceiver?.type as? BirSimpleType

    val dispatchReceiverTypeArguments = receiverType?.arguments ?: emptyList()

    if (typeParameters.isEmpty() && dispatchReceiverTypeArguments.isEmpty()) {
        return emptyMap()
    }

    val result = mutableMapOf<BirTypeParameterSymbol, BirType>()
    if (dispatchReceiverTypeArguments.isNotEmpty()) {
        val parentTypeParameters =
            if (function is BirConstructor) {
                val constructedClass = function.parentAsClass
                if (!constructedClass.isInner && dispatchReceiver != null) {
                    throw AssertionError("Non-inner class constructor reference with dispatch receiver:\n${this@getTypeSubstitutionMap.render()}")
                }
                extractTypeParameters(constructedClass.parent as BirClass)
            } else {
                extractTypeParameters(function.ancestors().firstIsInstance<BirClass>())
            }
        for ((index, typeParam) in parentTypeParameters.withIndex()) {
            dispatchReceiverTypeArguments[index].typeOrNull?.let {
                result[typeParam.symbol] = it
            }
        }
    }
    return (typeParameters.map { it.symbol } zip typeArguments.requireNoNulls()).toMap() + result
}

val BirFunctionReference.typeSubstitutionMap: Map<BirTypeParameterSymbol, BirType>
    get() = getTypeSubstitutionMap(symbol as BirFunction)

val BirFunctionAccessExpression.typeSubstitutionMap: Map<BirTypeParameterSymbol, BirType>
    get() = getTypeSubstitutionMap(symbol as BirFunction)

private fun Boolean.toInt(): Int = if (this) 1 else 0

val BirFunction.explicitParametersCount: Int
    get() = (dispatchReceiverParameter != null).toInt() + (extensionReceiverParameter != null).toInt() + valueParameters.size

val BirFunction.explicitParameters: List<BirValueParameter>
    get() = buildList(explicitParametersCount) {
        addIfNotNull(dispatchReceiverParameter)
        addAll(valueParameters.take(contextReceiverParametersCount))
        addIfNotNull(extensionReceiverParameter)
        addAll(valueParameters.drop(contextReceiverParametersCount))
    }

val BirFunction.allParameters: List<BirValueParameter>
    get() = if (this is BirConstructor) {
        listOf(this.constructedClass.thisReceiver ?: error(this.render())) + explicitParameters
    } else {
        explicitParameters
    }


/**
 * Binds the arguments explicitly represented in the IR to the parameters of the accessed function.
 * The arguments are to be evaluated in the same order as they appear in the resulting list.
 */
@Suppress("UNCHECKED_CAST")
fun BirMemberAccessExpression<*>.getArgumentsWithBir(): List<Pair<BirValueParameter, BirExpression>> {
    return getAllArgumentsWithBir().filter { it.second != null } as List<Pair<BirValueParameter, BirExpression>>
}

/**
 * Binds all arguments represented in the IR to the parameters of the accessed function.
 * The arguments are to be evaluated in the same order as they appear in the resulting list.
 */
fun BirMemberAccessExpression<*>.getAllArgumentsWithBir(): List<Pair<BirValueParameter, BirExpression?>> {
    val function = when (this) {
        is BirFunctionAccessExpression -> this.symbol as BirFunction
        is BirFunctionReference -> this.symbol as BirFunction
        is BirPropertyReference -> {
            assert(this.field == null) { "Field should be null to use `getArgumentsWithBir` on BirPropertyReference: ${this.dump()}}" }
            this.getter!!.owner
        }
        else -> error(this)
    }

    return getAllArgumentsWithBir(function)
}

/**
 * Binds all arguments represented in the IR to the parameters of the explicitly given function.
 * The arguments are to be evaluated in the same order as they appear in the resulting list.
 */
fun BirMemberAccessExpression<*>.getAllArgumentsWithBir(irFunction: BirFunction) = buildList {
    dispatchReceiver?.let { arg ->
        irFunction.dispatchReceiverParameter?.let { parameter -> add(parameter to arg) }
    }

    extensionReceiver?.let { arg ->
        irFunction.extensionReceiverParameter?.let { parameter -> add(parameter to arg) }
    }

    addAll(irFunction.valueParameters zip valueArguments)
}

val BirValueParameter.isVararg get() = this.varargElementType != null

fun BirFunctionAccessExpression.usesDefaultArguments(): Boolean =
    ((symbol as BirFunction).valueParameters zip valueArguments).any { (param, arg) -> arg == null && (!param.isVararg || param.defaultValue != null) }


fun BirExpression.isTrivial() =
    this is BirConst<*> ||
            this is BirGetValue ||
            this is BirGetObjectValue ||
            this is BirErrorExpression


fun BirValueParameter.hasDefaultValue(): Boolean = DFS.ifAny(
    listOf(this),
    { current -> (current.parent as? BirSimpleFunction)?.overriddenSymbols?.map { it.owner.valueParameters[current.index] } ?: listOf() },
    { current -> current.defaultValue != null }
)


fun BirMemberAccessExpression<*>.putArgument(parameter: BirValueParameter, argument: BirExpression?) {
    val calleeFunction = symbol.owner as? BirFunction
    when (parameter) {
        calleeFunction?.dispatchReceiverParameter -> dispatchReceiver = argument
        calleeFunction?.extensionReceiverParameter -> extensionReceiver = argument
        else -> valueArguments[parameter.index] = argument
    }
}

val BirBody.statements: List<BirStatement>
    get() = when (this) {
        is BirBlockBody -> statements
        is BirExpressionBody -> listOf(expression!!)
        is BirSyntheticBody -> error("Synthetic body contains no statements: $this")
        else -> error("Unknown subclass of BirBody: $this")
    }