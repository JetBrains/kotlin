/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.types.*

/**
 * Collects the upper bounds as [ConeClassLikeType] or [ConeErrorUnionType].
 */
fun ConeKotlinType?.collectUpperBounds(): Set<ConeRigidType> {
    if (this == null) return emptySet()

    val seen = mutableSetOf<Any>()

    fun collectErrorType(type: CEType): Sequence<CEType> {
        fun ConeKotlinType.stripToError(): CEType {
            return when (this) {
                is ConeFlexibleType -> upperBound.stripToError()
                is ConeErrorUnionType -> errorType
                else -> CEBotType
            }
        }

        if (!seen.add(type)) return emptySequence() // Avoid infinite recursion.

        return when (type) {
            is CEBotType -> sequenceOf(CEBotType)
            is CELookupTagBasedType -> when (type) {
                is CEClassifierType -> sequenceOf(type)
                is CETypeParameterType -> {
                    val symbol = type.lookupTag.typeParameterSymbol
                    symbol.resolvedBounds.asSequence().flatMap { collectErrorType(it.coneType.stripToError()) }
                }
                else -> error("missing branch for ${javaClass.name}")
            }
            is CETopType -> sequenceOf(CETopType)
            is CETypeVariableType -> {
                val symbol = (type.typeConstructor.originalTypeParameter as? ConeTypeParameterLookupTag)?.typeParameterSymbol
                    ?: return emptySequence()
                symbol.resolvedBounds.asSequence().flatMap { collectErrorType(it.coneType.stripToError()) }
            }
            is CEUnionType -> type.types.asSequence().flatMap(::collectErrorType)
        }
    }

    fun collect(type: ConeKotlinType): Sequence<ConeRigidType> {
        if (!seen.add(type)) return emptySequence() // Avoid infinite recursion.

        return when (type) {
            is ConeErrorType -> return emptySequence() // Ignore error types
            is ConeLookupTagBasedType -> when (type) {
                is ConeClassLikeType -> sequenceOf(type)
                is ConeTypeParameterType -> {
                    val symbol = type.lookupTag.typeParameterSymbol
                    symbol.resolvedBounds.asSequence().flatMap { collect(it.coneType) }
                }
                else -> error("missing branch for ${javaClass.name}")
            }
            is ConeTypeVariableType -> {
                val symbol = (type.typeConstructor.originalTypeParameter as? ConeTypeParameterLookupTag)?.typeParameterSymbol
                    ?: return emptySequence()
                symbol.resolvedBounds.asSequence().flatMap { collect(it.coneType) }
            }
            is ConeDefinitelyNotNullType -> collect(type.original)
            is ConeIntersectionType -> type.intersectedTypes.asSequence().flatMap(::collect)
            is ConeFlexibleType -> collect(type.upperBound)
            is ConeCapturedType -> type.constructor.supertypes?.asSequence()?.flatMap(::collect) ?: emptySequence()
            is ConeIntegerConstantOperatorType -> sequenceOf(type.getApproximatedType())
            is ConeStubType, is ConeIntegerLiteralConstantType -> {
                error("$type should not reach here")
            }
            is ConeErrorUnionType -> {
                val errorTypes = collectErrorType(type.errorType).toList()
                collect(type.valueType).map {
                    when (it) {
                        is ConeClassLikeType -> {
                            ConeErrorUnionType.create(
                                it, CEUnionType.create(errorTypes)
                            )
                        }
                        is ConeErrorUnionType -> {
                            val unitedErrorTypes = when (val et = it.errorType) {
                                is CEUnionType -> errorTypes + et.types
                                else -> errorTypes + et
                            }
                            ConeErrorUnionType.create(
                                it.valueType, CEUnionType.create(unitedErrorTypes)
                            )
                        }
                        else -> error("unexpected")
                    }
                }
            }
        }
    }

    val upperBounds = mutableSetOf<ConeRigidType>()
    collect(this).forEach { upperBounds.add(it) }
    return upperBounds
}

fun ConeRigidType.valueComponentOfUpperBound(): ConeClassLikeType {
    return when (this) {
        is ConeClassLikeType -> this
        is ConeErrorUnionType -> valueType as ConeClassLikeType
        else -> error("Unexpected type: $this")
    }
}

fun ConeRigidType.errorComponentOfUpperBound(): CEType {
    return when (this) {
        is ConeClassLikeType -> CEBotType
        is ConeErrorUnionType -> errorType
        else -> error("Unexpected type: $this")
    }
}
