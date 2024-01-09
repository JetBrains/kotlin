/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.types.utils

import org.jetbrains.kotlin.bir.symbols.BirClassSymbol
import org.jetbrains.kotlin.bir.symbols.BirScriptSymbol
import org.jetbrains.kotlin.bir.symbols.BirTypeParameterSymbol
import org.jetbrains.kotlin.bir.types.BirDynamicType
import org.jetbrains.kotlin.bir.types.BirErrorType
import org.jetbrains.kotlin.bir.types.BirSimpleType
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.ir.types.SimpleTypeNullability
import org.jetbrains.kotlin.types.DefinitelyNotNullType
import org.jetbrains.kotlin.types.TypeUtils


fun BirType.makeNotNull() = defineNullability(false)
fun BirType.makeNullable() = defineNullability(true)

fun BirType.defineNullability(newNullability: Boolean): BirType =
    when (this) {
        is BirSimpleType -> defineNullability(newNullability)
        else -> this
    }

fun BirSimpleType.defineNullability(newNullability: Boolean): BirSimpleType {
    val requiredNullability = if (newNullability) SimpleTypeNullability.MARKED_NULLABLE else SimpleTypeNullability.DEFINITELY_NOT_NULL
    return if (nullability == requiredNullability)
        this
    else
        buildSimpleType {
            nullability = requiredNullability
            kotlinType = originalKotlinType?.run {
                if (newNullability) {
                    TypeUtils.makeNullable(this)
                } else {
                    DefinitelyNotNullType.makeDefinitelyNotNull(this.unwrap()) ?: TypeUtils.makeNotNullable(this)
                }
            }
        }
}

fun BirType.mergeNullability(other: BirType) = when (other) {
    is BirSimpleType -> when (other.nullability) {
        SimpleTypeNullability.MARKED_NULLABLE -> makeNullable()
        SimpleTypeNullability.NOT_SPECIFIED -> this
        SimpleTypeNullability.DEFINITELY_NOT_NULL -> makeNotNull()
    }
    else -> this
}

fun BirType.isNullable(): Boolean =
    when (this) {
        is BirSimpleType -> when (val classifier = classifier) {
            is BirClassSymbol -> nullability == SimpleTypeNullability.MARKED_NULLABLE
            is BirTypeParameterSymbol -> when (nullability) {
                SimpleTypeNullability.MARKED_NULLABLE -> true
                // here is a bug, there should be .all check (not .any),
                // but fixing it is a breaking change, see KT-31545 for details
                SimpleTypeNullability.NOT_SPECIFIED -> classifier.owner.superTypes.any(BirType::isNullable)
                SimpleTypeNullability.DEFINITELY_NOT_NULL -> false
            }
            is BirScriptSymbol -> nullability == SimpleTypeNullability.MARKED_NULLABLE
            else -> error("Unsupported classifier: $classifier")
        }
        is BirDynamicType -> true
        is BirErrorType -> this.isMarkedNullable
        else -> false
    }

fun BirType.isMarkedNullable() = (this as? BirSimpleType)?.nullability == SimpleTypeNullability.MARKED_NULLABLE
fun BirSimpleType.isMarkedNullable() = nullability == SimpleTypeNullability.MARKED_NULLABLE
