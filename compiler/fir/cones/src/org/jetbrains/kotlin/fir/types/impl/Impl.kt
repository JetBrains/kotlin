/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types.impl

import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.ClassId

open class ConeClassTypeImpl(
    override val fqName: ClassId,
    override val typeArguments: List<ConeKotlinTypeProjection>
) : ConeClassType()

class ConeKotlinTypeProjectionInImpl(override val type: ConeKotlinType) : ConeKotlinTypeProjectionIn()

class ConeKotlinTypeProjectionOutImpl(override val type: ConeKotlinType) : ConeKotlinTypeProjectionOut()

class ConeKotlinErrorType(val reason: String) : ConeKotlinType() {
    override val typeArguments: List<ConeKotlinTypeProjection>
        get() = emptyList()

    override fun toString(): String {
        return "<ERROR TYPE: $reason>"
    }
}

class ConeAbbreviatedTypeImpl(
    override val abbreviationFqName: ClassId,
    override val typeArguments: List<ConeKotlinTypeProjection>,
    override val directExpansion: ConeKotlinType
) : ConeAbbreviatedType() {
    override val fqName: ClassId
        get() = abbreviationFqName
}