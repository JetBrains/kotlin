/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.types

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.FqNameEqualityChecker
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.impl.*
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.checker.convertVariance
import org.jetbrains.kotlin.types.model.*


private val NO_INFER_ANNOTATION_FQ_NAME = FqName("kotlin.internal.NoInfer")
private val EXACT_ANNOTATION_FQ_NAME = FqName("kotlin.internal.Exact")

interface IrTypeSystemContext : TypeSystemInferenceExtensionContext {

    val irBuiltIns: IrBuiltIns

    override fun KotlinTypeMarker.asSimpleType() = this as? SimpleTypeMarker

    override fun KotlinTypeMarker.asFlexibleType() = this as? IrDynamicType

    override fun KotlinTypeMarker.isError() = this is IrErrorType

    override fun SimpleTypeMarker.isStubType() = false

    override fun FlexibleTypeMarker.asDynamicType() = this as? IrDynamicType

    override fun FlexibleTypeMarker.asRawType(): RawTypeMarker? = null

    override fun FlexibleTypeMarker.upperBound(): SimpleTypeMarker {
        require(this is IrDynamicType)
        return irBuiltIns.anyNType as IrSimpleType
    }

    override fun FlexibleTypeMarker.lowerBound(): SimpleTypeMarker {
        require(this is IrDynamicType)
        return irBuiltIns.nothingType as IrSimpleType
    }

    override fun SimpleTypeMarker.asCapturedType(): CapturedTypeMarker? = null

    override fun SimpleTypeMarker.asDefinitelyNotNullType(): DefinitelyNotNullTypeMarker? = null

    override fun SimpleTypeMarker.isMarkedNullable(): Boolean = (this as IrSimpleType).containsNull()

    override fun SimpleTypeMarker.withNullability(nullable: Boolean): SimpleTypeMarker {
        val simpleType = this as IrSimpleType
        return if (simpleType.hasQuestionMark == nullable) simpleType
        else simpleType.run { IrSimpleTypeImpl(classifier, nullable, arguments, annotations) }
    }

    override fun SimpleTypeMarker.typeConstructor() = (this as IrSimpleType).classifier

    override fun KotlinTypeMarker.asTypeArgument() = this as IrTypeArgument

    override fun CapturedTypeMarker.lowerType(): KotlinTypeMarker? = error("Captured Type is not valid for IrTypes")

    override fun TypeArgumentMarker.isStarProjection() = this is IrStarProjection

    override fun TypeArgumentMarker.getVariance() = (this as IrTypeProjection).variance.convertVariance()

    override fun TypeArgumentMarker.getType() = (this as IrTypeProjection).type

    private fun getTypeParameters(typeConstructor: TypeConstructorMarker): List<IrTypeParameter> {
        return when (typeConstructor) {
            is IrTypeParameterSymbol -> emptyList()
            is IrClassSymbol -> typeConstructor.owner.typeParameters
            else -> error("unsupported type constructor")
        }
    }

    override fun TypeConstructorMarker.parametersCount() = getTypeParameters(this).size

    override fun TypeConstructorMarker.getParameter(index: Int) = getTypeParameters(this)[index].symbol

    override fun TypeConstructorMarker.supertypes(): Collection<KotlinTypeMarker> {
        return when (this) {
            is IrClassSymbol -> owner.superTypes
            is IrTypeParameterSymbol -> owner.superTypes
            else -> error("unsupported type constructor")
        }
    }

    override fun TypeConstructorMarker.isIntersection() = false

    override fun TypeConstructorMarker.isClassTypeConstructor() = this is IrClassSymbol

    override fun TypeParameterMarker.getVariance() = (this as IrTypeParameterSymbol).owner.variance.convertVariance()

    private fun getSuperTypes(typeParameterMarker: TypeParameterMarker) = (typeParameterMarker as IrTypeParameterSymbol).owner.superTypes

    override fun TypeParameterMarker.upperBoundCount() = getSuperTypes(this).size

    override fun TypeParameterMarker.getUpperBound(index: Int) = getSuperTypes(this)[index] as KotlinTypeMarker

    override fun TypeParameterMarker.getTypeConstructor() = this as IrTypeParameterSymbol

    override fun isEqualTypeConstructors(c1: TypeConstructorMarker, c2: TypeConstructorMarker) = FqNameEqualityChecker.areEqual(
        c1 as IrClassifierSymbol, c2 as IrClassifierSymbol
    )

    override fun TypeConstructorMarker.isDenotable() = true

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
     * See https://jetbrains.github.io/kotlin-spec/#type-capturing
     */

    override fun captureFromArguments(type: SimpleTypeMarker, status: CaptureStatus): SimpleTypeMarker? {
        // TODO: is that correct?
        require(type is IrSimpleType)

        val classifier = type.classifier as? IrClassSymbol ?: return null
        val typeArguments = type.arguments

        if (!classifier.isBound) return null

        val typeParameters = extractTypeParameters(classifier.owner)

        require(typeArguments.size == typeParameters.size)

        if (typeArguments.all { it is IrTypeProjection && it.variance == Variance.INVARIANT }) return type

        val newArguments = mutableListOf<IrTypeArgument>()
        for (index in typeArguments.indices) {
            val argument = typeArguments[index]
            val parameter = typeParameters[index]

            if (argument is IrTypeProjection && argument.variance == Variance.INVARIANT) {
                newArguments += argument
                continue
            }

            val additionalBounds =
                if (argument is IrTypeProjection && argument.variance == Variance.OUT_VARIANCE) listOf(argument.type) else emptyList()

            newArguments += makeTypeProjection(
                makeTypeIntersection(parameter.superTypes + additionalBounds),
                Variance.INVARIANT
            )
        }

        return IrSimpleTypeImpl(type.classifier, type.hasQuestionMark, newArguments, type.annotations)
    }

    override fun SimpleTypeMarker.asArgumentList() = this as IrSimpleType

    override fun TypeConstructorMarker.isAnyConstructor() = this === irBuiltIns.anyClass

    override fun TypeConstructorMarker.isNothingConstructor() = this === irBuiltIns.nothingClass

    override fun KotlinTypeMarker.isNotNullNothing(): Boolean {
        val simpleType = this as? IrSimpleType ?: return false
        return simpleType == irBuiltIns.nothingType && !simpleType.hasQuestionMark
    }

    override fun SimpleTypeMarker.isSingleClassifierType() = true

    override fun SimpleTypeMarker.possibleIntegerTypes() = irBuiltIns.run {
        setOf(byteType, shortType, intType, longType)
    }

    override fun TypeConstructorMarker.isIntegerLiteralTypeConstructor() = false

    override fun nullableNothingType() = irBuiltIns.nothingNType as IrSimpleType

    override fun nullableAnyType() = irBuiltIns.anyNType as IrSimpleType

    override fun nothingType() = irBuiltIns.nothingType as IrSimpleType

    override fun createFlexibleType(lowerBound: SimpleTypeMarker, upperBound: SimpleTypeMarker): KotlinTypeMarker {
        require(lowerBound == nothingType())
        require(upperBound == nullableAnyType())
        return IrDynamicTypeImpl(null, emptyList(), Variance.INVARIANT)
    }

    override fun createSimpleType(
        constructor: TypeConstructorMarker,
        arguments: List<TypeArgumentMarker>,
        nullable: Boolean
    ): SimpleTypeMarker = IrSimpleTypeImpl(constructor as IrClassifierSymbol, nullable, arguments.map { it as IrTypeArgument }, emptyList())

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

    override fun SimpleTypeMarker.typeDepth() = 2

    override fun KotlinTypeMarker.typeDepth() = if (this is IrStarProjection) 1 else 0

    override fun findCommonIntegerLiteralTypesSuperType(explicitSupertypes: List<SimpleTypeMarker>): SimpleTypeMarker? =
        irBuiltIns.intType as IrSimpleType

    override fun KotlinTypeMarker.contains(predicate: (KotlinTypeMarker) -> Boolean) = predicate(this)

    override fun TypeConstructorMarker.isUnitTypeConstructor() = isEqualTypeConstructors(this, irBuiltIns.unitClass)

    override fun TypeConstructorMarker.getApproximatedIntegerLiteralType(): KotlinTypeMarker = irBuiltIns.intType

    override fun Collection<KotlinTypeMarker>.singleBestRepresentative(): KotlinTypeMarker? {
        if (this.size == 1) return this.first()

        return this.firstOrNull { candidate ->
            this.all { other ->
                // We consider error types equal to anything here, so that intersections like
                // {Array<String>, Array<[ERROR]>} work correctly
                candidate == other || other is IrErrorType
            }
        }
    }

    override fun KotlinTypeMarker.isUnit() = (this as IrType) == irBuiltIns.unitType

    override fun KotlinTypeMarker.withNullability(nullable: Boolean) = if (this is IrSimpleType)
        IrSimpleTypeImpl(classifier, true, arguments, annotations)
    else this

    override fun KotlinTypeMarker.makeDefinitelyNotNullOrNotNull(): KotlinTypeMarker {
        error("Captured Type is not supported for IrType")
    }

    override fun SimpleTypeMarker.makeSimpleTypeDefinitelyNotNullOrNotNull(): SimpleTypeMarker {
        error("Captured Type is not supported for IrType")
    }

    override fun createCapturedType(
        constructorProjection: TypeArgumentMarker,
        constructorSupertypes: List<KotlinTypeMarker>,
        lowerType: KotlinTypeMarker?,
        captureStatus: CaptureStatus
    ): CapturedTypeMarker {
        error("Captured Type is not supported for IrType")
    }

    override fun createStubType(typeVariable: TypeVariableMarker) = error("Stub type is not supported for IrTypes")

    override fun KotlinTypeMarker.removeAnnotations() = with(this as IrType) {
        if (annotations.isEmpty()) this
        else {
            when {
                this is IrDynamicType -> IrDynamicTypeImpl(null, emptyList(), Variance.INVARIANT)
                this is IrSimpleType -> IrSimpleTypeImpl(classifier, hasQuestionMark, arguments, emptyList())
                else -> this
            }
        }
    }

    override fun SimpleTypeMarker.replaceArguments(newArguments: List<TypeArgumentMarker>): SimpleTypeMarker = with(this as IrSimpleType) {
        IrSimpleTypeImpl(classifier, hasQuestionMark, newArguments.map { it as IrTypeArgument }, annotations)
    }

    override fun KotlinTypeMarker.hasExactAnnotation(): Boolean = with(this as IrType) {
        annotations.hasAnnotation(EXACT_ANNOTATION_FQ_NAME)
    }

    override fun KotlinTypeMarker.hasNoInferAnnotation(): Boolean = with(this as IrType) {
        annotations.hasAnnotation(NO_INFER_ANNOTATION_FQ_NAME)
    }

    override fun TypeVariableMarker.freshTypeConstructor(): TypeConstructorMarker =
        error("freshTypeConstructor is not supported for IrType")

    override fun CapturedTypeMarker.typeConstructorProjection() =
        error("typeConstructorProjection is not supported for IrType")

    override fun KotlinTypeMarker.isNullableType() = this is IrDynamicType || this is IrSimpleType && hasQuestionMark

    override fun DefinitelyNotNullTypeMarker.original(): SimpleTypeMarker = error("DefinitelyNotNullTypeMarker is not supported for IrType")

    override fun typeSubstitutorByTypeConstructor(map: Map<TypeConstructorMarker, KotlinTypeMarker>): TypeSubstitutorMarker {
        TODO("not implemented")
    }

    override fun TypeSubstitutorMarker.safeSubstitute(type: KotlinTypeMarker): KotlinTypeMarker {
        TODO("not implemented")
    }

    override fun TypeVariableMarker.defaultType(): SimpleTypeMarker = error("defaultType is not supported for IrType")

    @Suppress("UNCHECKED_CAST")
    override fun intersectTypes(types: List<SimpleTypeMarker>): SimpleTypeMarker =
        makeTypeIntersection(types as List<IrType>) as SimpleTypeMarker


    @Suppress("UNCHECKED_CAST")
    override fun intersectTypes(types: List<KotlinTypeMarker>): KotlinTypeMarker =
        makeTypeIntersection(types as List<IrType>)

    override fun TypeConstructorMarker.isCapturedTypeConstructor(): Boolean = false

    override fun createErrorTypeWithCustomConstructor(debugName: String, constructor: TypeConstructorMarker): KotlinTypeMarker =
        TODO("IrTypeSystemContext doesn't support constraint system resolution")

    override fun CapturedTypeMarker.captureStatus(): CaptureStatus =
        error("Captured type is unsupported in IR")
}

fun extractTypeParameters(klass: IrDeclarationParent): List<IrTypeParameter> {
    val result = mutableListOf<IrTypeParameter>()
    var current: IrDeclarationParent? = klass
    while (current != null) {
//        result += current.typeParameters
        (current as? IrTypeParametersContainer)?.let { result += it.typeParameters }
        current =
            when (current) {
                is IrField -> current.parent
                is IrClass -> when {
                    current.isInner -> current.parent as IrClass
                    current.visibility == Visibilities.LOCAL -> current.parent
                    else -> null
                }
                is IrConstructor -> current.parent as IrClass
                is IrFunction -> if (current.visibility == Visibilities.LOCAL || current.dispatchReceiverParameter != null) {
                    current.parent
                } else null
                else -> null
            }
    }
    return result
}