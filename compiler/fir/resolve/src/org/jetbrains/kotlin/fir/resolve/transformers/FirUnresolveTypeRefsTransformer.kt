/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.transformSingle

/** Recursively replaces all resolved type refs with their delegated type refs */
internal object FirUnresolveTypeRefsTransformer : FirTransformer<Nothing?>() {
    override fun <E : FirElement> transformElement(element: E, data: Nothing?): E {
        @Suppress("UNCHECKED_CAST")
        return element.transformChildren(this, data) as E
    }

    override fun transformResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef, data: Nothing?): FirTypeRef {
        return resolvedTypeRef.delegatedTypeRef?.transformSingle(this, data) ?: resolvedTypeRef
    }
}
