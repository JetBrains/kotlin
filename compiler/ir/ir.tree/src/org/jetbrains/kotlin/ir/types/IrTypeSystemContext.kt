/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.types

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeIntersection
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.checker.convertVariance
import org.jetbrains.kotlin.types.model.*

interface IrTypeSystemContext : TypeSystemContext {

    val irBuiltIns: IrBuiltIns

    override fun KotlinTypeMarker.asSimpleType() = this as? SimpleTypeMarker

    override fun KotlinTypeMarker.asFlexibleType() = this as? IrDynamicType

    override fun KotlinTypeMarker.isError() = this is IrErrorType

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

    override fun SimpleTypeMarker.isMarkedNullable(): Boolean = (this as IrSimpleType).hasQuestionMark

    override fun SimpleTypeMarker.withNullability(nullable: Boolean): SimpleTypeMarker {
        val simpleType = this as IrSimpleType
        return if (simpleType.hasQuestionMark == nullable) simpleType
        else simpleType.run { IrSimpleTypeImpl(classifier, nullable, arguments, annotations) }
    }

    override fun SimpleTypeMarker.typeConstructor() = (this as IrSimpleType).classifier

    override fun SimpleTypeMarker.argumentsCount(): Int = (this as IrSimpleType).arguments.size

    override fun SimpleTypeMarker.getArgument(index: Int): TypeArgumentMarker {
        val simpleType = this as IrSimpleType
        return simpleType.arguments[index]
    }

    override fun KotlinTypeMarker.asTypeArgument() = this as IrTypeArgument

    override fun CapturedTypeMarker.lowerType(): KotlinTypeMarker? = error("Captured Type is not valid for IrTypes")

    override fun TypeArgumentMarker.isStarProjection() = this is IrStarProjection

    override fun TypeArgumentMarker.getVariance() = (this as IrTypeProjection).variance.convertVariance()

    override fun TypeArgumentMarker.getType() = (this as IrTypeProjection).type

    private fun getTypeParameters(typeConstructor: TypeConstructorMarker): List<IrTypeParameter> {
        val ownTypeParameterContainer = when(typeConstructor) {
            is IrTypeParameterSymbol -> (typeConstructor.owner.parent as IrTypeParametersContainer)
            is IrClassSymbol -> typeConstructor.owner
            else -> error("unsupported type constructor")
        }

        return ownTypeParameterContainer.typeParameters
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

    override fun isEqualTypeConstructors(c1: TypeConstructorMarker, c2: TypeConstructorMarker) = (c1 === c2)

    override fun TypeConstructorMarker.isDenotable() = false

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

        val classifier = type.classifier as IrClassSymbol
        val typeArguments = type.arguments

        if (!classifier.isBound) return null

        val typeParameters = classifier.owner.typeParameters

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

            newArguments += makeTypeProjection(makeTypeIntersection(parameter.superTypes + additionalBounds), Variance.INVARIANT)
        }

        return IrSimpleTypeImpl(type.classifier, type.hasQuestionMark, newArguments, type.annotations)
    }

    override fun SimpleTypeMarker.asArgumentList() = this as IrSimpleType

    override fun TypeConstructorMarker.isAnyConstructor() = this === irBuiltIns.anyClass

    override fun TypeConstructorMarker.isNothingConstructor() = this === irBuiltIns.nothingClass

    override fun KotlinTypeMarker.isNotNullNothing(): Boolean {
        val simpleType = this as? IrSimpleType ?: return false
        return simpleType.classifier === irBuiltIns.nothingClass && !simpleType.hasQuestionMark
    }

    override fun SimpleTypeMarker.isSingleClassifierType() = true
}