/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.types

import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.types.KaTypeNullability
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeNullability

internal interface KaFirType : KaLifetimeOwner {
    val coneType: ConeKotlinType
}

internal fun KaFirType.typeEquals(other: Any?): Boolean {
    if (other !is KaFirType) return false
    return this.coneType == other.coneType
}

internal fun KaFirType.typeHashcode(): Int = coneType.hashCode()

internal fun ConeNullability.asKtNullability(): KaTypeNullability = when (this) {
    ConeNullability.NULLABLE -> KaTypeNullability.NULLABLE
    ConeNullability.UNKNOWN -> KaTypeNullability.UNKNOWN
    ConeNullability.NOT_NULL -> KaTypeNullability.NON_NULLABLE
}
