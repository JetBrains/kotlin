/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.types

import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeOwner
import org.jetbrains.kotlin.analysis.api.types.KtTypeNullability
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeNullability

internal interface KtFirType : KtLifetimeOwner {
    val coneType: ConeKotlinType
}

internal fun KtFirType.typeEquals(other: Any?): Boolean {
    if (other !is KtFirType) return false
    return this.coneType == other.coneType
}

internal fun KtFirType.typeHashcode(): Int = coneType.hashCode()

internal fun ConeNullability.asKtNullability(): KtTypeNullability = when (this) {
    ConeNullability.NULLABLE -> KtTypeNullability.NULLABLE
    ConeNullability.UNKNOWN -> KtTypeNullability.UNKNOWN
    ConeNullability.NOT_NULL -> KtTypeNullability.NON_NULLABLE
}
