/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.types

import org.jetbrains.kotlin.bir.BirBuiltIns
import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.declarations.*
import org.jetbrains.kotlin.bir.expressions.BirConst
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.symbols.BirClassSymbol
import org.jetbrains.kotlin.bir.symbols.BirClassifierSymbol
import org.jetbrains.kotlin.bir.symbols.BirTypeParameterSymbol
import org.jetbrains.kotlin.bir.symbols.ownerIfBound
import org.jetbrains.kotlin.bir.types.utils.*
import org.jetbrains.kotlin.bir.util.*
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrCapturedType
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
import org.jetbrains.kotlin.bir.types.utils.defineNullability as birDefineNullability
import org.jetbrains.kotlin.bir.types.utils.isMarkedNullable as birIsMarkedNullable
import org.jetbrains.kotlin.bir.types.utils.isPrimitiveType as birTypePredicates_isPrimitiveType

interface BirTypeSystemContext : TypeSystemContext, TypeSystemCommonSuperTypesContext, TypeSystemCommonBackendContext {
    val birBuiltIns: BirBuiltIns

    override fun KotlinTypeMarker.asSimpleType() = this as? SimpleTypeMarker

    override fun KotlinTypeMarker.asFlexibleType(): FlexibleTypeMarker? = this as? FlexibleTypeMarker

    override fun KotlinTypeMarker.isError() = this is BirErrorType

    override fun SimpleTypeMarker.isStubType() = false

    override fun SimpleTypeMarker.isStubTypeForVariableInSubtyping() = false

    override fun SimpleTypeMarker.isStubTypeForBuilderInference() = false

    override fun TypeConstructorMarker.unwrapStubTypeVariableConstructor(): TypeConstructorMarker = this

    override fun FlexibleTypeMarker.asDynamicType() = this as? BirDynamicType

    override fun KotlinTypeMarker.isRawType(): Boolean = false

    override fun FlexibleTypeMarker.upperBound(): SimpleTypeMarker {
        return when (this) {
            is BirDynamicType -> birBuiltIns.anyNType as BirSimpleType
            else -> error("Unexpected flexible type ${this::class.java.simpleName}: $this")
        }
    }

    override fun FlexibleTypeMarker.lowerBound(): SimpleTypeMarker {
        return when (this) {
            is BirDynamicType -> birBuiltIns.nothingType as BirSimpleType
            else -> error("Unexpected flexible type ${this::class.java.simpleName}: $this")
        }
    }

    override fun SimpleTypeMarker.asCapturedType(): CapturedTypeMarker? = this as? BirCapturedType

    override fun SimpleTypeMarker.asDefinitelyNotNullType(): DefinitelyNotNullTypeMarker? = null

    override fun SimpleTypeMarker.isMarkedNullable(): Boolean = this is BirSimpleType && this.birIsMarkedNullable()

    override fun KotlinTypeMarker.isMarkedNullable(): Boolean = this is BirSimpleType && this.birIsMarkedNullable()

    override fun SimpleTypeMarker.withNullability(nullable: Boolean): SimpleTypeMarker {
        val simpleType = this as BirSimpleType
        return simpleType.birDefineNullability(nullable)
    }

    override fun SimpleTypeMarker.typeConstructor(): TypeConstructorMarker = when (this) {
        is BirCapturedType -> constructor
        is BirSimpleType -> classifier
        is BirErrorType -> symbol
        else -> error("Unknown type constructor")
    }

    override fun CapturedTypeMarker.typeConstructor(): CapturedTypeConstructorMarker =
        (this as BirCapturedType).constructor

    // Note: `isProjectionNotNull` is used inside inference along with intersection types.
    // BirTypes are not used in type inference and do not have intersection type so implemenation is default (false)
    override fun CapturedTypeMarker.isProjectionNotNull(): Boolean = false

    override fun CapturedTypeMarker.captureStatus(): CaptureStatus =
        (this as BirCapturedType).captureStatus

    override fun CapturedTypeMarker.isOldCapturedType(): Boolean = false

    override fun CapturedTypeConstructorMarker.projection(): TypeArgumentMarker =
        (this as BirCapturedType.Constructor).argument

    override fun KotlinTypeMarker.argumentsCount(): Int =
        when (this) {
            is BirSimpleType -> arguments.size
            else -> 0
        }

    override fun KotlinTypeMarker.getArgument(index: Int): TypeArgumentMarker =
        when (this) {
            is BirSimpleType ->
                if (index >= arguments.size)
                    error("No argument $index in type '${this.render()}'")
                else
                    arguments[index]
            else ->
                error("Type $this has no arguments")
        }

    override fun KotlinTypeMarker.getArguments(): List<TypeArgumentMarker> =
        when (this) {
            is BirSimpleType -> arguments
            else -> error("Type $this has no arguments")
        }

    override fun KotlinTypeMarker.asTypeArgument() = this as BirTypeArgument

    override fun CapturedTypeMarker.lowerType(): KotlinTypeMarker? = (this as BirCapturedType).lowerType

    override fun TypeArgumentMarker.isStarProjection() = this is BirStarProjection

    override fun TypeArgumentMarker.getVariance(): TypeVariance =
        (this as? BirTypeProjection)?.variance?.convertVariance() ?: TypeVariance.OUT

    override fun TypeArgumentMarker.replaceType(newType: KotlinTypeMarker): TypeArgumentMarker =
        BirTypeProjectionImpl(newType as BirType, (this as BirTypeProjection).variance)

    override fun TypeArgumentMarker.getType() = (this as BirTypeProjection).type

    private fun getTypeParameters(typeConstructor: TypeConstructorMarker): List<BirTypeParameter> {
        return when (typeConstructor) {
            is BirTypeParameterSymbol -> emptyList()
            is BirClassSymbol -> extractTypeParameters(typeConstructor.owner)
            else -> error("unsupported type constructor")
        }
    }

    override fun TypeConstructorMarker.parametersCount() = getTypeParameters(this).size

    override fun TypeConstructorMarker.getParameter(index: Int) = getTypeParameters(this)[index].symbol

    override fun TypeConstructorMarker.getParameters() = getTypeParameters(this).map { it.symbol }

    override fun TypeConstructorMarker.supertypes(): Collection<KotlinTypeMarker> {
        return when (this) {
            is BirCapturedType.Constructor -> superTypes
            is BirClassSymbol -> owner.superTypes
            is BirTypeParameterSymbol -> owner.superTypes
            else -> error("unsupported type constructor")
        }
    }

    override fun TypeConstructorMarker.isIntersection() = false

    override fun TypeConstructorMarker.isClassTypeConstructor() = this is BirClassSymbol

    override fun TypeConstructorMarker.isInterface(): Boolean {
        return (this as? BirClassSymbol)?.owner?.kind == ClassKind.INTERFACE
    }

    override fun TypeConstructorMarker.isTypeParameterTypeConstructor(): Boolean = this is BirTypeParameterSymbol

    override fun TypeParameterMarker.getVariance() = (this as BirTypeParameter).variance.convertVariance()

    private fun getSuperTypes(typeParameterMarker: TypeParameterMarker) = (typeParameterMarker as BirTypeParameter).superTypes

    override fun TypeParameterMarker.upperBoundCount() = getSuperTypes(this).size

    override fun TypeParameterMarker.getUpperBound(index: Int) = getSuperTypes(this)[index] as KotlinTypeMarker

    override fun TypeParameterMarker.getUpperBounds() = getSuperTypes(this) as List<KotlinTypeMarker>

    override fun TypeParameterMarker.getTypeConstructor() = this as BirTypeParameterSymbol

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
        if (c1 is BirClassifierSymbol && c2 is BirClassifierSymbol) {
            FqNameEqualityChecker.areEqual(c1, c2)
        } else c1 === c2

    override fun TypeConstructorMarker.isDenotable() = this !is BirCapturedType.Constructor

    override fun TypeConstructorMarker.isCommonFinalClassConstructor(): Boolean {
        val classSymbol = this as? BirClassSymbol ?: return false
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
        require(type is BirSimpleType)

        if (type is BirCapturedType) return null

        val classifier = type.classifier as? BirClassSymbol ?: return null
        val typeArguments = type.arguments

        if (!classifier.isBound) return null

        val typeParameters = extractTypeParameters(classifier.owner)
        require(typeArguments.size == typeParameters.size)

        if (typeArguments.all { it is BirTypeProjection && it.variance == Variance.INVARIANT }) return type

        val capturedTypes = typeArguments.mapIndexed { index, argument ->
            if (argument is BirTypeProjection && argument.variance == Variance.INVARIANT) {
                null
            } else {
                val lowerType = if (argument is BirTypeProjection && argument.variance == Variance.IN_VARIANCE) {
                    argument.type
                } else null

                BirCapturedType(
                    status, lowerType, argument, typeParameters[index], SimpleTypeNullability.DEFINITELY_NOT_NULL, emptyList(), null
                )
            }
        }

        val typeSubstitutor = BirCapturedTypeSubstitutor(typeParameters.memoryOptimizedMap { it.symbol }, typeArguments, capturedTypes, birBuiltIns)

        val newArguments = typeArguments.mapIndexed { index, oldArgument ->
            val capturedType = capturedTypes[index]

            if (capturedType == null) {
                assert(oldArgument is BirTypeProjection && oldArgument.variance == Variance.INVARIANT)
                oldArgument
            } else {
                val capturedSuperTypes = mutableListOf<BirType>()
                typeParameters[index].superTypes.mapTo(capturedSuperTypes) {
                    typeSubstitutor.substitute(it)
                }

                if (oldArgument is BirTypeProjection && oldArgument.variance == Variance.OUT_VARIANCE) {
                    capturedSuperTypes += oldArgument.type
                }

                capturedType.constructor.initSuperTypes(capturedSuperTypes)

                makeTypeProjection(capturedType, Variance.INVARIANT)
            }
        }

        return BirSimpleTypeImpl(type.classifier, type.nullability, newArguments, type.annotations)
    }

    override fun SimpleTypeMarker.asArgumentList() = this as BirSimpleType

    override fun TypeConstructorMarker.isAnyConstructor(): Boolean =
        this is BirClassSymbol && isClassWithFqName(StandardNames.FqNames.any)

    override fun TypeConstructorMarker.isNothingConstructor(): Boolean =
        this is BirClassSymbol && isClassWithFqName(StandardNames.FqNames.nothing)

    override fun TypeConstructorMarker.isArrayConstructor(): Boolean =
        this is BirClassSymbol && isClassWithFqName(StandardNames.FqNames.array)

    override fun SimpleTypeMarker.isSingleClassifierType() = true

    override fun SimpleTypeMarker.possibleIntegerTypes() = birBuiltIns.run {
        setOf(byteType, shortType, intType, longType)
    }

    override fun TypeConstructorMarker.isIntegerLiteralTypeConstructor(): Boolean = false
    override fun TypeConstructorMarker.isIntegerLiteralConstantTypeConstructor(): Boolean = false
    override fun TypeConstructorMarker.isIntegerConstantOperatorTypeConstructor(): Boolean = false

    override fun TypeConstructorMarker.isLocalType(): Boolean {
        if (this !is BirClassSymbol) return false
        return this.owner.classId?.isLocal == true
    }

    override fun TypeConstructorMarker.isAnonymous(): Boolean {
        if (this !is BirClassSymbol) return false
        return this.owner.classId?.shortClassName == SpecialNames.ANONYMOUS
    }

    override val TypeVariableTypeConstructorMarker.typeParameter: TypeParameterMarker?
        get() = error("Type variables is unsupported in IR")

    override fun createFlexibleType(lowerBound: SimpleTypeMarker, upperBound: SimpleTypeMarker): KotlinTypeMarker {
        require(lowerBound.isNothing())
        require(upperBound is BirType && upperBound.isNullableAny())
        return BirDynamicType(null, emptyList(), Variance.INVARIANT)
    }

    override fun createSimpleType(
        constructor: TypeConstructorMarker,
        arguments: List<TypeArgumentMarker>,
        nullable: Boolean,
        isExtensionFunction: Boolean,
        attributes: List<AnnotationMarker>?,
    ): SimpleTypeMarker {
        val ourAnnotations = attributes?.memoryOptimizedFilterIsInstance<BirConstructorCall>()
        require(ourAnnotations?.size == attributes?.size)
        return BirSimpleTypeImpl(
            constructor as BirClassifierSymbol,
            if (nullable) SimpleTypeNullability.MARKED_NULLABLE else SimpleTypeNullability.DEFINITELY_NOT_NULL,
            arguments.memoryOptimizedMap { it as BirTypeArgument },
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
        makeTypeProjection(type as BirType, variance.convertVariance())

    override fun createStarProjection(typeParameter: TypeParameterMarker) = BirStarProjection

    override fun KotlinTypeMarker.canHaveUndefinedNullability() = this is BirSimpleType && classifier is BirTypeParameterSymbol

    override fun SimpleTypeMarker.isExtensionFunction(): Boolean {
        require(this is BirSimpleType)
        return this.hasAnnotation(StandardNames.FqNames.extensionFunctionType)
    }

    override fun SimpleTypeMarker.typeDepth(): Int {
        val maxInArguments = (this as BirSimpleType).arguments.maxOfOrNull {
            if (it is BirStarProjection) 1 else it.getType().typeDepth()
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
        birBuiltIns.intType as BirSimpleType

    override fun KotlinTypeMarker.replaceCustomAttributes(newAttributes: List<AnnotationMarker>): KotlinTypeMarker = this

    override fun unionTypeAttributes(types: List<KotlinTypeMarker>): List<AnnotationMarker> = emptyList()

    override fun KotlinTypeMarker.isNullableType(): Boolean =
        this is BirType && isNullable()

    @Suppress("UNCHECKED_CAST")
    override fun intersectTypes(types: Collection<SimpleTypeMarker>): SimpleTypeMarker =
        makeTypeIntersection(types as Collection<BirType>) as SimpleTypeMarker

    @Suppress("UNCHECKED_CAST")
    override fun intersectTypes(types: Collection<KotlinTypeMarker>): KotlinTypeMarker =
        makeTypeIntersection(types as Collection<BirType>)

    override fun SimpleTypeMarker.isPrimitiveType(): Boolean =
        this is BirSimpleType && birTypePredicates_isPrimitiveType()

    override fun KotlinTypeMarker.getAttributes(): List<AnnotationMarker> {
        require(this is BirType)
        return this.annotations.memoryOptimizedMap { object : AnnotationMarker, BirElement by it {} }
    }

    override fun KotlinTypeMarker.hasCustomAttributes(): Boolean {
        return false
    }

    override fun KotlinTypeMarker.getCustomAttributes(): List<AnnotationMarker> {
        require(this is BirType)
        return emptyList()
    }

    override fun createErrorType(debugName: String, delegatedType: SimpleTypeMarker?): SimpleTypeMarker {
        TODO("BirTypeSystemContext doesn't support constraint system resolution")
    }

    override fun createUninferredType(constructor: TypeConstructorMarker): KotlinTypeMarker =
        TODO("BirTypeSystemContext doesn't support constraint system resolution")

    override fun nullableAnyType() = birBuiltIns.anyNType as BirSimpleType

    override fun nullableNothingType() = birBuiltIns.nothingNType as BirSimpleType

    override fun nothingType() = birBuiltIns.nothingType as BirSimpleType

    override fun anyType() = birBuiltIns.anyType as BirSimpleType

    override fun arrayType(componentType: KotlinTypeMarker): SimpleTypeMarker =
        birBuiltIns.arrayClass.createType(componentType as BirType)

    override fun KotlinTypeMarker.isArrayOrNullableArray(): Boolean =
        (this as BirType).isClassType(IdSignatureValues.array)

    override fun TypeConstructorMarker.isFinalClassOrEnumEntryOrAnnotationClassConstructor(): Boolean {
        val symbol = this as BirClassifierSymbol
        return symbol is BirClassSymbol && symbol.owner.let {
            it.modality == Modality.FINAL && it.kind != ClassKind.ENUM_CLASS
        }
    }

    override fun KotlinTypeMarker.hasAnnotation(fqName: FqName): Boolean =
        (this as BirAnnotationContainer).hasAnnotation(fqName)

    override fun KotlinTypeMarker.getAnnotationFirstArgumentValue(fqName: FqName): Any? =
        (this as? BirType)?.annotations?.firstOrNull { annotation ->
            annotation.symbol.owner.parentAsClass.hasEqualFqName(fqName)
        }?.run {
            (valueArguments.firstOrNull() as? BirConst<*>)?.value
        }

    override fun TypeConstructorMarker.getTypeParameterClassifier(): TypeParameterMarker? =
        this as? BirTypeParameterSymbol

    override fun TypeConstructorMarker.isInlineClass(): Boolean =
        (this as? BirClassSymbol)?.owner?.isSingleFieldValueClass == true

    override fun TypeConstructorMarker.isMultiFieldValueClass(): Boolean =
        (this as? BirClassSymbol)?.owner?.isMultiFieldValueClass == true

    override fun TypeConstructorMarker.getValueClassProperties(): List<Pair<Name, SimpleTypeMarker>>? =
        (this as? BirClassSymbol)?.owner?.valueClassRepresentation?.underlyingPropertyNamesToTypes

    override fun TypeConstructorMarker.isInnerClass(): Boolean =
        (this as? BirClassSymbol)?.owner?.isInner == true

    override fun TypeParameterMarker.getRepresentativeUpperBound(): KotlinTypeMarker =
        (this as BirTypeParameterSymbol).owner.superTypes.firstOrNull {
            val irClass = it.classOrNull?.owner ?: return@firstOrNull false
            irClass.kind != ClassKind.INTERFACE && irClass.kind != ClassKind.ANNOTATION_CLASS
        } ?: owner.superTypes.first()

    override fun KotlinTypeMarker.getUnsubstitutedUnderlyingType(): KotlinTypeMarker? =
        (this as BirType).classOrNull?.owner?.inlineClassRepresentation?.underlyingType

    override fun KotlinTypeMarker.getSubstitutedUnderlyingType(): KotlinTypeMarker? =
        getUnsubstitutedUnderlyingType()?.let { type ->
            // Taking only the type parameters of the class (and not its outer classes) is OK since inner classes are always top level
            BirTypeSubstitutor(
                (this as BirType).getClass()!!.typeParameters.memoryOptimizedMap { it.symbol },
                (this as? BirSimpleType)?.arguments.orEmpty(),
                birBuiltIns
            ).substitute(type as BirType)
        }

    override fun TypeConstructorMarker.getPrimitiveType(): PrimitiveType? =
        getNameForClassUnderKotlinPackage()?.let(PrimitiveType::getByShortName)

    override fun TypeConstructorMarker.getPrimitiveArrayType(): PrimitiveType? =
        getNameForClassUnderKotlinPackage()?.let(PrimitiveType::getByShortArrayName)

    private fun TypeConstructorMarker.getNameForClassUnderKotlinPackage(): String? {
        if (this !is BirClassSymbol) return null

        val signature = signature?.asPublic()
        return if (signature != null) {
            if (signature.packageFqName == StandardNames.BUILT_INS_PACKAGE_NAME.asString())
                signature.declarationFqName
            else null
        } else {
            val parent = owner.parent
            if (parent is BirPackageFragment && parent.packageFqName == StandardNames.BUILT_INS_PACKAGE_FQ_NAME)
                owner.name.asString()
            else null
        }
    }

    override fun TypeConstructorMarker.isUnderKotlinPackage(): Boolean {
        val declaration: BirDeclaration = (this as? BirClass) ?: return false
        return declaration.ancestors()
            .takeWhile { it is BirClass }
            .any { (it.parent as? BirPackageFragment)?.packageFqName?.startsWith(StandardNames.BUILT_INS_PACKAGE_NAME) == true }
    }

    override fun TypeConstructorMarker.getClassFqNameUnsafe(): FqNameUnsafe? =
        (this as BirClassSymbol).owner.fqNameWhenAvailable?.toUnsafe()

    override fun TypeParameterMarker.getName(): Name =
        (this as BirTypeParameterSymbol).owner.name

    override fun TypeParameterMarker.isReified(): Boolean =
        (this as BirTypeParameterSymbol).owner.isReified

    override fun KotlinTypeMarker.isInterfaceOrAnnotationClass(): Boolean {
        val irClass = (this as BirType).classOrNull?.owner
        return irClass != null && (irClass.kind == ClassKind.INTERFACE || irClass.kind == ClassKind.ANNOTATION_CLASS)
    }


    override fun newTypeCheckerState(
        errorTypesEqualToAnything: Boolean,
        stubTypesEqualToAnything: Boolean,
    ): TypeCheckerState = createBirTypeCheckerState(this)

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
        require(type is BirSimpleType)
        val parameters = extractTypeParameters(type.classifier as BirClass).memoryOptimizedMap { it.symbol }
        val typeSubstitutor = BirTypeSubstitutor(parameters, type.arguments, birBuiltIns)

        return object : TypeCheckerState.SupertypesPolicy.DoCustomTransform() {
            override fun transformType(state: TypeCheckerState, type: KotlinTypeMarker): SimpleTypeMarker {
                require(type is BirType)
                return typeSubstitutor.substitute(type) as BirSimpleType
            }
        }
    }

    override fun KotlinTypeMarker.isTypeVariableType(): Boolean {
        return false
    }

    override fun typeSubstitutorByTypeConstructor(map: Map<TypeConstructorMarker, KotlinTypeMarker>): TypeSubstitutorMarker {
        val typeParameters = mutableListOf<BirTypeParameterSymbol>()
        val typeArguments = mutableListOf<BirTypeArgument>()
        for ((key, value) in map) {
            typeParameters += key as BirTypeParameterSymbol
            typeArguments += value as BirTypeArgument
        }
        return BirTypeSubstitutor(typeParameters, typeArguments, birBuiltIns)
    }

    override fun createEmptySubstitutor(): TypeSubstitutorMarker {
        return BirTypeSubstitutor(emptyList(), emptyList(), birBuiltIns)
    }

    override fun TypeSubstitutorMarker.safeSubstitute(type: KotlinTypeMarker): KotlinTypeMarker {
        require(this is AbstractBirTypeSubstitutor)
        require(type is BirType)
        return substitute(type)
    }
}

fun extractTypeParameters(parent: BirElement): List<BirTypeParameter> {
    val result = mutableListOf<BirTypeParameter>()
    var current: BirElement? = parent
    while (current != null) {
        (current as? BirTypeParametersContainer)?.let { result += it.typeParameters }
        current =
            when (current) {
                is BirField -> current.parent
                is BirClass -> when {
                    current.isInner -> current.parent as BirClass
                    current.visibility == DescriptorVisibilities.LOCAL -> current.parent
                    else -> null
                }
                is BirConstructor -> current.parent as BirClass
                is BirFunction ->
                    if (current.visibility == DescriptorVisibilities.LOCAL || current.dispatchReceiverParameter != null)
                        current.parent
                    else
                        null
                else -> null
            }
    }
    return result.compactIfPossible()
}

class BirTypeSystemContextImpl(
    override val birBuiltIns: BirBuiltIns,
) : BirTypeSystemContext
