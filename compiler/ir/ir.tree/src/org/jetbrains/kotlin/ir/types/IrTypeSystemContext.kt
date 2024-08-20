/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.types

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.FqNameEqualityChecker
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.impl.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.types.TypeCheckerState
import org.jetbrains.kotlin.types.TypeSystemCommonBackendContext
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.model.*
import org.jetbrains.kotlin.utils.compactIfPossible
import org.jetbrains.kotlin.utils.memoryOptimizedFilterIsInstance
import org.jetbrains.kotlin.utils.memoryOptimizedMap
import org.jetbrains.kotlin.ir.types.isMarkedNullable as irIsMarkedNullable
import org.jetbrains.kotlin.ir.types.isPrimitiveType as irTypePredicates_isPrimitiveType
import org.jetbrains.kotlin.ir.types.makeNotNull as irMakeNotNull
import org.jetbrains.kotlin.ir.types.makeNullable as irMakeNullable

interface IrTypeSystemContext : TypeSystemContext, TypeSystemCommonSuperTypesContext, TypeSystemCommonBackendContext {

    val irBuiltIns: IrBuiltIns

    override fun KotlinTypeMarker.asSimpleType() = this as? SimpleTypeMarker

    override fun KotlinTypeMarker.asFlexibleType(): FlexibleTypeMarker? = this as? FlexibleTypeMarker

    override fun KotlinTypeMarker.isError() = this is IrErrorType

    override fun SimpleTypeMarker.isStubType() = false

    override fun SimpleTypeMarker.isStubTypeForVariableInSubtyping() = false

    override fun SimpleTypeMarker.isStubTypeForBuilderInference() = false

    override fun TypeConstructorMarker.unwrapStubTypeVariableConstructor(): TypeConstructorMarker = this

    override fun FlexibleTypeMarker.asDynamicType() = this as? IrDynamicType

    override fun KotlinTypeMarker.isRawType(): Boolean = false

    override fun FlexibleTypeMarker.upperBound(): SimpleTypeMarker {
        return when (this) {
            is IrDynamicType -> irBuiltIns.anyNType as IrSimpleType
            else -> error("Unexpected flexible type ${this::class.java.simpleName}: $this")
        }
    }

    override fun FlexibleTypeMarker.lowerBound(): SimpleTypeMarker {
        return when (this) {
            is IrDynamicType -> irBuiltIns.nothingType as IrSimpleType
            else -> error("Unexpected flexible type ${this::class.java.simpleName}: $this")
        }
    }

    override fun SimpleTypeMarker.asCapturedType(): CapturedTypeMarker? = this as? IrCapturedType

    override fun SimpleTypeMarker.asDefinitelyNotNullType(): DefinitelyNotNullTypeMarker? = null

    override fun SimpleTypeMarker.isMarkedNullable(): Boolean = this is IrSimpleType && this.irIsMarkedNullable()

    override fun KotlinTypeMarker.isMarkedNullable(): Boolean = this is IrSimpleType && this.irIsMarkedNullable()

    override fun SimpleTypeMarker.withNullability(nullable: Boolean): SimpleTypeMarker {
        val simpleType = this as IrSimpleType
        return (if (nullable) simpleType.irMakeNullable() else simpleType.irMakeNotNull()) as IrSimpleType
    }

    override fun SimpleTypeMarker.typeConstructor(): TypeConstructorMarker = when (this) {
        is IrCapturedType -> constructor
        is IrSimpleType -> classifier
        is IrErrorType -> symbol
        else -> error("Unknown type constructor")
    }

    override fun CapturedTypeMarker.typeConstructor(): CapturedTypeConstructorMarker =
        (this as IrCapturedType).constructor

    // Note: `isProjectionNotNull` is used inside inference along with intersection types.
    // IrTypes are not used in type inference and do not have intersection type so implemenation is default (false)
    override fun CapturedTypeMarker.isProjectionNotNull(): Boolean = false

    override fun CapturedTypeMarker.captureStatus(): CaptureStatus =
        (this as IrCapturedType).captureStatus

    override fun CapturedTypeMarker.isOldCapturedType(): Boolean = false

    override fun CapturedTypeConstructorMarker.projection(): TypeArgumentMarker =
        (this as IrCapturedType.Constructor).argument

    override fun KotlinTypeMarker.argumentsCount(): Int =
        when (this) {
            is IrSimpleType -> arguments.size
            else -> 0
        }

    override fun KotlinTypeMarker.getArgument(index: Int): TypeArgumentMarker =
        when (this) {
            is IrSimpleType ->
                if (index >= arguments.size)
                    error("No argument $index in type '${this.render()}'")
                else
                    arguments[index]
            else ->
                error("Type $this has no arguments")
        }

    override fun KotlinTypeMarker.getArguments(): List<TypeArgumentMarker> =
        when (this) {
            is IrSimpleType -> arguments
            else -> error("Type $this has no arguments")
        }

    override fun KotlinTypeMarker.asTypeArgument() = this as IrTypeArgument

    override fun CapturedTypeMarker.lowerType(): KotlinTypeMarker? = (this as IrCapturedType).lowerType

    override fun TypeArgumentMarker.isStarProjection() = this is IrStarProjection

    override fun TypeArgumentMarker.getVariance(): TypeVariance =
        (this as? IrTypeProjection)?.variance?.convertVariance() ?: TypeVariance.OUT

    override fun TypeArgumentMarker.replaceType(newType: KotlinTypeMarker): TypeArgumentMarker {
        require(this is IrTypeArgument)
        return when (this) {
            is IrStarProjection -> this
            is IrTypeProjection -> IrTypeProjectionImpl(newType as IrType, this.variance)
        }
    }

    override fun TypeArgumentMarker.getType() = (this as IrTypeProjection).type

    private fun getTypeParameters(typeConstructor: TypeConstructorMarker): List<IrTypeParameter> {
        return when (typeConstructor) {
            is IrTypeParameterSymbol -> emptyList()
            is IrClassSymbol -> extractTypeParameters(typeConstructor.owner)
            else -> error("unsupported type constructor")
        }
    }

    override fun TypeConstructorMarker.parametersCount() = getTypeParameters(this).size

    override fun TypeConstructorMarker.getParameter(index: Int) = getTypeParameters(this)[index].symbol

    override fun TypeConstructorMarker.getParameters() = getTypeParameters(this).map { it.symbol }

    override fun TypeConstructorMarker.supertypes(): Collection<KotlinTypeMarker> {
        return when (this) {
            is IrCapturedType.Constructor -> superTypes
            is IrClassSymbol -> owner.superTypes
            is IrTypeParameterSymbol -> owner.superTypes
            else -> error("unsupported type constructor")
        }
    }

    override fun TypeConstructorMarker.isIntersection() = false

    override fun TypeConstructorMarker.isClassTypeConstructor() = this is IrClassSymbol

    override fun TypeConstructorMarker.isInterface(): Boolean {
        return (this as? IrClassSymbol)?.owner?.isInterface == true
    }

    override fun TypeConstructorMarker.isTypeParameterTypeConstructor(): Boolean = this is IrTypeParameterSymbol

    override fun TypeParameterMarker.getVariance() = (this as IrTypeParameterSymbol).owner.variance.convertVariance()

    private fun getSuperTypes(typeParameterMarker: TypeParameterMarker) = (typeParameterMarker as IrTypeParameterSymbol).owner.superTypes

    override fun TypeParameterMarker.upperBoundCount() = getSuperTypes(this).size

    override fun TypeParameterMarker.getUpperBound(index: Int) = getSuperTypes(this)[index] as KotlinTypeMarker

    override fun TypeParameterMarker.getUpperBounds() = getSuperTypes(this) as List<KotlinTypeMarker>

    override fun TypeParameterMarker.getTypeConstructor() = this as IrTypeParameterSymbol

    private fun KotlinTypeMarker.containsTypeConstructor(constructor: TypeConstructorMarker): Boolean {
        if (this.typeConstructor() == constructor) return true

        for (i in 0 until this.argumentsCount()) {
            val typeArgument = this.getArgument(i).takeIf { !it.isStarProjection() } ?: continue
            if (typeArgument.getType().containsTypeConstructor(constructor)) return true
        }

        return false
    }

    override fun TypeParameterMarker.hasRecursiveBounds(selfConstructor: TypeConstructorMarker?): Boolean {
        for (i in 0 until this.upperBoundCount()) {
            val upperBound = this.getUpperBound(i)
            if (upperBound.containsTypeConstructor(this.getTypeConstructor()) && (selfConstructor == null || upperBound.typeConstructor() == selfConstructor)) {
                return true
            }
        }

        return false
    }

    override fun areEqualTypeConstructors(c1: TypeConstructorMarker, c2: TypeConstructorMarker): Boolean =
        if (c1 is IrClassifierSymbol && c2 is IrClassifierSymbol) {
            FqNameEqualityChecker.areEqual(c1 , c2)
        } else c1 === c2

    override fun TypeConstructorMarker.isDenotable() = this !is IrCapturedType.Constructor

    override fun TypeConstructorMarker.isCommonFinalClassConstructor(): Boolean {
        val classSymbol = this as? IrClassSymbol ?: return false
        return classSymbol.owner.run { modality == Modality.FINAL && kind != ClassKind.ENUM_CLASS && kind != ClassKind.ANNOTATION_CLASS }
    }

    /*
     * fun <T> foo(x: Array<T>) {}
     * fun bar(y: Array<out CharSequence>) {
     *   foo(y)
     * }
     *
     * In this case Captured Type of `y` would be `Array<CharSequence>`
     *
     * See https://kotlinlang.org/spec/type-system.html#type-capturing
     */
    override fun captureFromArguments(type: SimpleTypeMarker, status: CaptureStatus): SimpleTypeMarker? {
        require(type is IrSimpleType)

        if (type is IrCapturedType) return null

        val classifier = type.classifier as? IrClassSymbol ?: return null
        val typeArguments = type.arguments

        if (!classifier.isBound) return null

        val typeParameters = extractTypeParameters(classifier.owner)
        require(typeArguments.size == typeParameters.size)

        if (typeArguments.all { it is IrTypeProjection && it.variance == Variance.INVARIANT }) return type

        val capturedTypes = typeArguments.mapIndexed { index, argument ->
            if (argument is IrTypeProjection && argument.variance == Variance.INVARIANT) {
                null
            } else {
                val lowerType = if (argument is IrTypeProjection && argument.variance == Variance.IN_VARIANCE) {
                    argument.type
                } else null

                IrCapturedType(
                    status, lowerType, argument, typeParameters[index], SimpleTypeNullability.DEFINITELY_NOT_NULL, emptyList(), null
                )
            }
        }

        val typeSubstitutor = IrCapturedTypeSubstitutor(typeParameters.memoryOptimizedMap { it.symbol }, typeArguments, capturedTypes)

        val newArguments = typeArguments.mapIndexed { index, oldArgument ->
            val capturedType = capturedTypes[index]

            if (capturedType == null) {
                assert(oldArgument is IrTypeProjection && oldArgument.variance == Variance.INVARIANT)
                oldArgument
            } else {
                val capturedSuperTypes = mutableListOf<IrType>()
                typeParameters[index].superTypes.mapTo(capturedSuperTypes) {
                    typeSubstitutor.substitute(it)
                }

                if (oldArgument is IrTypeProjection && oldArgument.variance == Variance.OUT_VARIANCE) {
                    capturedSuperTypes += oldArgument.type
                }

                capturedType.constructor.initSuperTypes(capturedSuperTypes)

                makeTypeProjection(capturedType, Variance.INVARIANT)
            }
        }

        return IrSimpleTypeImpl(type.classifier, type.nullability, newArguments, type.annotations)
    }

    override fun SimpleTypeMarker.asArgumentList() = this as IrSimpleType

    override fun TypeConstructorMarker.isAnyConstructor(): Boolean =
        this is IrClassSymbol && isClassWithFqName(StandardNames.FqNames.any)

    override fun TypeConstructorMarker.isNothingConstructor(): Boolean =
        this is IrClassSymbol && isClassWithFqName(StandardNames.FqNames.nothing)

    override fun TypeConstructorMarker.isArrayConstructor(): Boolean =
        this is IrClassSymbol && isClassWithFqName(StandardNames.FqNames.array)

    override fun SimpleTypeMarker.isSingleClassifierType() = true

    override fun SimpleTypeMarker.possibleIntegerTypes() = irBuiltIns.run {
        setOf(byteType, shortType, intType, longType)
    }

    override fun TypeConstructorMarker.isIntegerLiteralTypeConstructor(): Boolean = false
    override fun TypeConstructorMarker.isIntegerLiteralConstantTypeConstructor(): Boolean = false
    override fun TypeConstructorMarker.isIntegerConstantOperatorTypeConstructor(): Boolean = false

    override fun TypeConstructorMarker.isLocalType(): Boolean {
        if (this !is IrClassSymbol) return false
        return this.owner.classId?.isLocal == true
    }

    override fun TypeConstructorMarker.isAnonymous(): Boolean {
        if (this !is IrClassSymbol) return false
        return this.owner.classId?.shortClassName == SpecialNames.ANONYMOUS
    }

    override val TypeVariableTypeConstructorMarker.typeParameter: TypeParameterMarker?
        get() = error("Type variables is unsupported in IR")

    override fun createFlexibleType(lowerBound: SimpleTypeMarker, upperBound: SimpleTypeMarker): KotlinTypeMarker {
        require(lowerBound.isNothing())
        require(upperBound is IrType && upperBound.isNullableAny())
        return IrDynamicTypeImpl(emptyList(), Variance.INVARIANT)
    }

    override fun createSimpleType(
        constructor: TypeConstructorMarker,
        arguments: List<TypeArgumentMarker>,
        nullable: Boolean,
        isExtensionFunction: Boolean,
        attributes: List<AnnotationMarker>?
    ): SimpleTypeMarker {
        val ourAnnotations = attributes?.memoryOptimizedFilterIsInstance<IrConstructorCall>()
        require(ourAnnotations?.size == attributes?.size)
        return IrSimpleTypeImpl(
            constructor as IrClassifierSymbol,
            if (nullable) SimpleTypeNullability.MARKED_NULLABLE else SimpleTypeNullability.DEFINITELY_NOT_NULL,
            arguments.memoryOptimizedMap { it as IrTypeArgument },
            ourAnnotations ?: emptyList()
        )
    }

    private fun TypeVariance.convertVariance(): Variance {
        return when (this) {
            TypeVariance.INV -> Variance.INVARIANT
            TypeVariance.IN -> Variance.IN_VARIANCE
            TypeVariance.OUT -> Variance.OUT_VARIANCE
        }
    }

    override fun createTypeArgument(type: KotlinTypeMarker, variance: TypeVariance): TypeArgumentMarker =
        makeTypeProjection(type as IrType, variance.convertVariance())

    override fun createStarProjection(typeParameter: TypeParameterMarker) = IrStarProjectionImpl

    override fun KotlinTypeMarker.canHaveUndefinedNullability() = this is IrSimpleType && classifier is IrTypeParameterSymbol

    override fun SimpleTypeMarker.isExtensionFunction(): Boolean {
        require(this is IrSimpleType)
        return this.hasAnnotation(StandardNames.FqNames.extensionFunctionType)
    }

    override fun SimpleTypeMarker.typeDepth(): Int {
        val maxInArguments = (this as IrSimpleType).arguments.maxOfOrNull {
            if (it is IrStarProjection) 1 else it.getType().typeDepth()
        } ?: 0

        return maxInArguments + 1
    }

    override fun TypeConstructorMarker.toErrorType(): SimpleTypeMarker {
        throw IllegalStateException("Should not be called")
    }

    override fun TypeConstructorMarker.isError(): Boolean {
        throw IllegalStateException("Should not be called")
    }

    override fun findCommonIntegerLiteralTypesSuperType(explicitSupertypes: List<SimpleTypeMarker>): SimpleTypeMarker? =
        irBuiltIns.intType as IrSimpleType

    override fun KotlinTypeMarker.replaceCustomAttributes(newAttributes: List<AnnotationMarker>): KotlinTypeMarker = this

    override fun unionTypeAttributes(types: List<KotlinTypeMarker>): List<AnnotationMarker> = emptyList()

    override fun KotlinTypeMarker.isNullableType(): Boolean =
        this is IrType && isNullable()

    @Suppress("UNCHECKED_CAST")
    override fun intersectTypes(types: Collection<SimpleTypeMarker>): SimpleTypeMarker =
        makeTypeIntersection(types as Collection<IrType>) as SimpleTypeMarker

    @Suppress("UNCHECKED_CAST")
    override fun intersectTypes(types: Collection<KotlinTypeMarker>): KotlinTypeMarker =
        makeTypeIntersection(types as Collection<IrType>)

    override fun SimpleTypeMarker.isPrimitiveType(): Boolean =
        this is IrSimpleType && irTypePredicates_isPrimitiveType()

    override fun KotlinTypeMarker.getAttributes(): List<AnnotationMarker> {
        require(this is IrType)
        return this.annotations
    }

    override fun createErrorType(debugName: String, delegatedType: SimpleTypeMarker?): SimpleTypeMarker {
        TODO("IrTypeSystemContext doesn't support constraint system resolution")
    }

    override fun createUninferredType(constructor: TypeConstructorMarker): KotlinTypeMarker =
        TODO("IrTypeSystemContext doesn't support constraint system resolution")

    override fun nullableAnyType() = irBuiltIns.anyNType as IrSimpleType

    override fun nullableNothingType() = irBuiltIns.nothingNType as IrSimpleType

    override fun nothingType() = irBuiltIns.nothingType as IrSimpleType

    override fun anyType() = irBuiltIns.anyType as IrSimpleType

    override fun arrayType(componentType: KotlinTypeMarker): SimpleTypeMarker =
        irBuiltIns.arrayClass.typeWith(componentType as IrType)

    override fun KotlinTypeMarker.isArrayOrNullableArray(): Boolean =
        (this as IrType).isArray() || isNullableArray()

    override fun TypeConstructorMarker.isFinalClassOrEnumEntryOrAnnotationClassConstructor(): Boolean {
        val symbol = this as IrClassifierSymbol
        return symbol is IrClassSymbol && symbol.owner.let {
            it.modality == Modality.FINAL && !it.isEnumClass
        }
    }

    override fun KotlinTypeMarker.hasAnnotation(fqName: FqName): Boolean =
        (this as IrAnnotationContainer).hasAnnotation(fqName)

    override fun KotlinTypeMarker.getAnnotationFirstArgumentValue(fqName: FqName): Any? =
        (this as? IrType)?.annotations?.firstOrNull { annotation ->
            annotation.symbol.owner.parentAsClass.hasEqualFqName(fqName)
        }?.run {
            if (valueArgumentsCount > 0) (getValueArgument(0) as? IrConst)?.value else null
        }

    override fun TypeConstructorMarker.getTypeParameterClassifier(): TypeParameterMarker? =
        this as? IrTypeParameterSymbol

    override fun TypeConstructorMarker.isInlineClass(): Boolean =
        (this as? IrClassSymbol)?.owner?.isSingleFieldValueClass == true

    override fun TypeConstructorMarker.isMultiFieldValueClass(): Boolean =
        (this as? IrClassSymbol)?.owner?.isMultiFieldValueClass == true

    override fun TypeConstructorMarker.getValueClassProperties(): List<Pair<Name, SimpleTypeMarker>>? =
        (this as? IrClassSymbol)?.owner?.valueClassRepresentation?.underlyingPropertyNamesToTypes

    override fun TypeConstructorMarker.isInnerClass(): Boolean =
        (this as? IrClassSymbol)?.owner?.isInner == true

    override fun TypeParameterMarker.getRepresentativeUpperBound(): KotlinTypeMarker =
        (this as IrTypeParameterSymbol).owner.superTypes.firstOrNull {
            val irClass = it.classOrNull?.owner ?: return@firstOrNull false
            irClass.kind != ClassKind.INTERFACE && irClass.kind != ClassKind.ANNOTATION_CLASS
        } ?: owner.superTypes.first()

    override fun KotlinTypeMarker.getUnsubstitutedUnderlyingType(): KotlinTypeMarker? =
        (this as IrType).classOrNull?.owner?.inlineClassRepresentation?.underlyingType

    override fun KotlinTypeMarker.getSubstitutedUnderlyingType(): KotlinTypeMarker? =
        getUnsubstitutedUnderlyingType()?.let { type ->
            // Taking only the type parameters of the class (and not its outer classes) is OK since inner classes are always top level
            IrTypeSubstitutor(
                (this as IrType).getClass()!!.typeParameters.memoryOptimizedMap { it.symbol },
                (this as? IrSimpleType)?.arguments.orEmpty(),
            ).substitute(type as IrType)
        }

    override fun TypeConstructorMarker.getPrimitiveType(): PrimitiveType? =
        getNameForClassUnderKotlinPackage()?.let(PrimitiveType::getByShortName)

    override fun TypeConstructorMarker.getPrimitiveArrayType(): PrimitiveType? =
        getNameForClassUnderKotlinPackage()?.let(PrimitiveType::getByShortArrayName)

    private fun TypeConstructorMarker.getNameForClassUnderKotlinPackage(): String? {
        if (this !is IrClassSymbol) return null

        val signature = signature?.asPublic()
        return if (signature != null) {
            if (signature.packageFqName == StandardNames.BUILT_INS_PACKAGE_NAME.asString())
                signature.declarationFqName
            else null
        } else {
            val parent = owner.parent
            if (parent is IrPackageFragment && parent.packageFqName == StandardNames.BUILT_INS_PACKAGE_FQ_NAME)
                owner.name.asString()
            else null
        }
    }

    override fun TypeConstructorMarker.isUnderKotlinPackage(): Boolean {
        var declaration: IrDeclaration = (this as? IrClassifierSymbol)?.owner as? IrClass ?: return false
        while (true) {
            val parent = declaration.parent
            if (parent is IrPackageFragment) {
                return parent.packageFqName.startsWith(StandardNames.BUILT_INS_PACKAGE_NAME)
            }
            declaration = parent as? IrDeclaration ?: return false
        }
    }

    override fun TypeConstructorMarker.getClassFqNameUnsafe(): FqNameUnsafe? =
        (this as IrClassSymbol).owner.fqNameWhenAvailable?.toUnsafe()

    override fun TypeParameterMarker.getName(): Name =
        (this as IrTypeParameterSymbol).owner.name

    override fun TypeParameterMarker.isReified(): Boolean =
        (this as IrTypeParameterSymbol).owner.isReified

    override fun KotlinTypeMarker.isInterfaceOrAnnotationClass(): Boolean {
        val irClass = (this as IrType).classOrNull?.owner
        return irClass != null && (irClass.isInterface || irClass.isAnnotationClass)
    }


    override fun newTypeCheckerState(
        errorTypesEqualToAnything: Boolean,
        stubTypesEqualToAnything: Boolean
    ): TypeCheckerState = createIrTypeCheckerState(this)

    override fun KotlinTypeMarker.isUninferredParameter(): Boolean = false
    override fun KotlinTypeMarker.withNullability(nullable: Boolean): KotlinTypeMarker {
        if (this.isSimpleType()) {
            return this.asSimpleType()!!.withNullability(nullable)
        } else {
            error("withNullability for non-simple types is not supported in IR")
        }
    }

    override fun captureFromExpression(type: KotlinTypeMarker): KotlinTypeMarker? =
        error("Captured type is unsupported in IR")

    override fun DefinitelyNotNullTypeMarker.original(): SimpleTypeMarker =
        error("DefinitelyNotNullTypeMarker.original() type is unsupported in IR")

    override fun KotlinTypeMarker.makeDefinitelyNotNullOrNotNull(preserveAttributes: Boolean): KotlinTypeMarker {
        error("makeDefinitelyNotNullOrNotNull is not supported in IR")
    }

    override fun SimpleTypeMarker.makeSimpleTypeDefinitelyNotNullOrNotNull(): SimpleTypeMarker {
        error("makeSimpleTypeDefinitelyNotNullOrNotNull is not yet supported in IR")
    }

    override fun substitutionSupertypePolicy(type: SimpleTypeMarker): TypeCheckerState.SupertypesPolicy {
        require(type is IrSimpleType)
        val parameters = extractTypeParameters((type.classifier as IrClassSymbol).owner).memoryOptimizedMap { it.symbol }
        val typeSubstitutor = IrTypeSubstitutor(parameters, type.arguments)

        return object : TypeCheckerState.SupertypesPolicy.DoCustomTransform() {
            override fun transformType(state: TypeCheckerState, type: KotlinTypeMarker): SimpleTypeMarker {
                require(type is IrType)
                return typeSubstitutor.substitute(type) as IrSimpleType
            }
        }
    }

    override fun KotlinTypeMarker.isTypeVariableType(): Boolean {
        return false
    }

    override fun typeSubstitutorByTypeConstructor(map: Map<TypeConstructorMarker, KotlinTypeMarker>): TypeSubstitutorMarker {
        @Suppress("UNCHECKED_CAST")
        return IrTypeSubstitutor(map as Map<IrTypeParameterSymbol, IrTypeArgument>)
    }

    override fun createEmptySubstitutor(): TypeSubstitutorMarker {
        return IrTypeSubstitutor(emptyMap())
    }

    override fun TypeSubstitutorMarker.safeSubstitute(type: KotlinTypeMarker): KotlinTypeMarker {
        require(this is AbstractIrTypeSubstitutor)
        require(type is IrType)
        return substitute(type)
    }

    override fun supportsImprovedVarianceInCst(): Boolean {
        return irBuiltIns.languageVersionSettings.supportsFeature(LanguageFeature.ImprovedVarianceInCst)
    }
}

fun extractTypeParameters(parent: IrDeclarationParent): List<IrTypeParameter> {
    val result = mutableListOf<IrTypeParameter>()
    var current: IrDeclarationParent? = parent
    while (current != null) {
        (current as? IrTypeParametersContainer)?.let { result += it.typeParameters }
        current =
            when (current) {
                is IrField -> current.parent
                is IrClass -> when {
                    current.isInner -> current.parent as IrClass
                    current.visibility == DescriptorVisibilities.LOCAL -> current.parent
                    else -> null
                }
                is IrConstructor -> current.parent as IrClass
                is IrFunction ->
                    if (current.visibility == DescriptorVisibilities.LOCAL || current.dispatchReceiverParameter != null)
                        current.parent
                    else
                        null
                else -> null
            }
    }
    return result.compactIfPossible()
}


class IrTypeSystemContextImpl(override val irBuiltIns: IrBuiltIns) : IrTypeSystemContext
