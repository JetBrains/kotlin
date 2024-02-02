/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.types.utils

import org.jetbrains.kotlin.bir.declarations.BirClass
import org.jetbrains.kotlin.bir.declarations.BirSimpleFunction
import org.jetbrains.kotlin.bir.declarations.BirTypeParameter
import org.jetbrains.kotlin.bir.declarations.BirTypeParametersContainer
import org.jetbrains.kotlin.bir.symbols.BirClassSymbol
import org.jetbrains.kotlin.bir.symbols.BirClassifierSymbol
import org.jetbrains.kotlin.bir.symbols.BirScriptSymbol
import org.jetbrains.kotlin.bir.symbols.BirTypeParameterSymbol
import org.jetbrains.kotlin.bir.types.*
import org.jetbrains.kotlin.bir.types.impl.BirTypeBase
import org.jetbrains.kotlin.bir.util.*
import org.jetbrains.kotlin.bir.util.defaultType
import org.jetbrains.kotlin.ir.types.SimpleTypeNullability
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.memoryOptimizedMap

val BirType.classifierOrFail: BirClassifierSymbol
    get() = classifierOrNull ?: error("Can't get classifier of ${render()}")

val BirType.classifierOrNull: BirClassifierSymbol?
    get() = when (this) {
        is BirSimpleType -> classifier
        else -> null
    }

val BirType.classOrNull: BirClassSymbol?
    get() =
        when (val classifier = classifierOrNull) {
            is BirClassSymbol -> classifier
            is BirScriptSymbol -> classifier.owner.targetClass
            else -> null
        }

fun BirType.getClass(): BirClass? = classOrNull?.owner

val BirType.classFqName: FqName?
    get() = classOrNull?.owner?.fqNameWhenAvailable

val BirTypeArgument.typeOrNull: BirType? get() = (this as? BirTypeProjection)?.type

val BirTypeArgument.typeOrFail: BirType
    get() {
        require(this is BirTypeProjection) { "Type argument should be of type `BirTypeProjection`, but was `${this::class}` instead" }
        return this.type
    }

val BirType.originalKotlinType: KotlinType?
    get() = (this as? BirTypeBase)?.kotlinType


fun BirClassSymbol.createType(hasQuestionMark: Boolean, arguments: List<BirTypeArgument>): BirSimpleType =
    BirSimpleTypeImpl(
        this,
        hasQuestionMark,
        arguments,
        emptyList()
    )

fun BirClassifierSymbol.createType(vararg arguments: BirType): BirSimpleType = createType(arguments.toList())

@JvmName("createTypeWithBirTypesAsArguments")
fun BirClassifierSymbol.createType(arguments: List<BirType>): BirSimpleType =
    BirSimpleTypeImpl(
        this,
        SimpleTypeNullability.NOT_SPECIFIED,
        arguments.memoryOptimizedMap { makeTypeProjection(it, Variance.INVARIANT) },
        emptyList()
    )

fun BirClassifierSymbol.createType(arguments: List<BirTypeArgument>): BirSimpleType =
    BirSimpleTypeImpl(this, SimpleTypeNullability.NOT_SPECIFIED, arguments, emptyList())

fun BirClassifierSymbol.createTypeWithParameters(parameters: List<BirTypeParameter>): BirSimpleType =
    createType(parameters.map { it.defaultType })

val BirClassifierSymbol.defaultType: BirType
    get() = when (this) {
        is BirClassSymbol -> owner.defaultType
        is BirTypeParameterSymbol -> owner.defaultType
        else -> error("Unexpected classifier symbol type $this")
    }

val BirTypeParameter.defaultType: BirType
    get() = BirSimpleTypeImpl(
        symbol,
        SimpleTypeNullability.NOT_SPECIFIED,
        arguments = emptyList(),
        annotations = emptyList()
    )


val BirClassifierSymbol.superTypes: List<BirType>
    get() = when (this) {
        is BirClassSymbol -> owner.superTypes
        is BirTypeParameterSymbol -> owner.superTypes
        else -> emptyList()
    }


val BirClassSymbol.starProjectedType: BirSimpleType
    get() = BirSimpleTypeImpl(
        this,
        SimpleTypeNullability.NOT_SPECIFIED,
        arguments = List(owner.typeConstructorParameters.count()) { BirStarProjection },
        annotations = emptyList()
    )

val BirClass.typeConstructorParameters: Sequence<BirTypeParameter>
    get() = generateSequence(this as BirTypeParametersContainer) { current ->
        val parent = current.parent as? BirTypeParametersContainer
        when {
            parent is BirSimpleFunction && parent.isPropertyAccessor -> {
                // KT-42151
                // Property type parameters for local classes declared inside property accessors are not captured in FE descriptors.
                // In order to match type parameters against type arguments in IR types translated from KotlinTypes,
                // we should stop on property accessor here.
                // NB this can potentially cause problems with inline properties with reified type parameters.
                // Ideally this should be fixed in FE.
                null
            }
            current is BirSimpleFunction && current.isStatic -> {
                // Static functions don't capture type parameters.
                null
            }
            current.isAnonymousObject -> {
                // Anonymous classes don't capture type parameters.
                null
            }
            parent is BirClass && current is BirClass && !current.isInner ->
                null
            // Inline class constructor inherits the same type parameters as the inline class itself
            current is BirSimpleFunction && current.name.asString() == "constructor-impl" ->
                null
            else ->
                parent
        }
    }.flatMap { it.typeParameters }

fun BirClassifierSymbol.typeWithParameters(parameters: List<BirTypeParameter>): BirSimpleType =
    typeWith(parameters.map { it.defaultType })

fun BirClassifierSymbol.typeWith(vararg arguments: BirType): BirSimpleType = typeWith(arguments.toList())

fun BirClassifierSymbol.typeWith(arguments: List<BirType>): BirSimpleType =
    BirSimpleTypeImpl(
        this,
        SimpleTypeNullability.NOT_SPECIFIED,
        arguments.memoryOptimizedMap { makeTypeProjection(it, Variance.INVARIANT) },
        emptyList()
    )

fun BirClassifierSymbol.typeWithArguments(arguments: List<BirTypeArgument>): BirSimpleType =
    BirSimpleTypeImpl(this, SimpleTypeNullability.NOT_SPECIFIED, arguments, emptyList())
