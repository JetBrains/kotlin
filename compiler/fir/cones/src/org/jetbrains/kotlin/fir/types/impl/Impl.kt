/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types.impl

import org.jetbrains.kotlin.fir.UnambiguousFqName
import org.jetbrains.kotlin.fir.types.*

class ConeClassTypeImpl(
    override val fqName: UnambiguousFqName,
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