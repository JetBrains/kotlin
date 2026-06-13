/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.model.structure

import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.types.KotlinType

@K1Deprecation
sealed class ESType {
    abstract fun toKotlinType(builtIns: KotlinBuiltIns): KotlinType
}

@K1Deprecation
abstract class ESBuiltInType : ESType()

@K1Deprecation
object ESAnyType : ESBuiltInType() {
    override fun toKotlinType(builtIns: KotlinBuiltIns): KotlinType = builtIns.anyType
}

@K1Deprecation
object ESNullableAnyType : ESBuiltInType() {
    override fun toKotlinType(builtIns: KotlinBuiltIns): KotlinType = builtIns.nullableAnyType
}

@K1Deprecation
object ESNullableNothingType : ESBuiltInType() {
    override fun toKotlinType(builtIns: KotlinBuiltIns): KotlinType = builtIns.nullableNothingType
}

@K1Deprecation
object ESNothingType : ESBuiltInType() {
    override fun toKotlinType(builtIns: KotlinBuiltIns): KotlinType = builtIns.nothingType
}

@K1Deprecation
object ESBooleanType : ESType() {
    override fun toKotlinType(builtIns: KotlinBuiltIns): KotlinType = builtIns.booleanType
}

@K1Deprecation
class ESKotlinType(val type: KotlinType) : ESType() {
    override fun toKotlinType(builtIns: KotlinBuiltIns): KotlinType = type
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ESKotlinType

        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        return type.hashCode()
    }
}


@K1Deprecation
fun KotlinType.toESType() = when {
    KotlinBuiltIns.isBoolean(this) -> ESBooleanType
    KotlinBuiltIns.isAny(this) -> ESAnyType
    KotlinBuiltIns.isNullableAny(this) -> ESNullableAnyType
    KotlinBuiltIns.isNothing(this) -> ESNothingType
    KotlinBuiltIns.isNullableNothing(this) -> ESNullableNothingType
    else -> ESKotlinType(this)
}

@K1Deprecation
fun ESType?.isBoolean(): Boolean = this is ESBooleanType