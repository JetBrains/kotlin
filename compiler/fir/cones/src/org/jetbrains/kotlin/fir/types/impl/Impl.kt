/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types.impl

import org.jetbrains.kotlin.fir.types.ConeAttributes
import org.jetbrains.kotlin.fir.types.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.util.WeakPair

class ConeClassLikeTypeImpl(
    override val lookupTag: ConeClassLikeLookupTag,
    typeArguments: Array<out ConeTypeProjection>,
    override val isMarkedNullable: Boolean,
    override val attributes: ConeAttributes = ConeAttributes.Empty
) : ConeClassLikeType() {
    override val typeArguments: Array<out ConeTypeProjection> = if (typeArguments.isEmpty()) EMPTY_ARRAY else typeArguments

    // Cached expanded type and the relevant session
    var cachedExpandedType: WeakPair<*, ConeClassLikeType>? = null
}
