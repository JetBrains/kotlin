/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccess
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.impl.FirResolvedQualifierImpl
import org.jetbrains.kotlin.fir.references.impl.FirSimpleNamedReference
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.resolve.transformers.PackageOrClass
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.resultType
import org.jetbrains.kotlin.fir.resolve.transformers.resolveToPackageOrClass
import org.jetbrains.kotlin.fir.resolve.typeForQualifier
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class FirQualifiedNameResolver(components: BodyResolveComponents) : BodyResolveComponents by components {
    private var qualifierStack = mutableListOf<Name>()
    private var qualifierPartsToDrop = 0

    fun reset() {
        qualifierStack.clear()
        qualifierPartsToDrop = 0
    }

    fun initProcessingQualifiedAccess(qualifiedAccess: FirQualifiedAccess, callee: FirSimpleNamedReference) {
        if (qualifiedAccess.safe || callee.name.isSpecial) {
            qualifierStack.clear()
        } else {
            qualifierStack.add(callee.name)
        }
    }

    fun replacedQualifier(qualifiedAccess: FirQualifiedAccess): FirStatement? =
        if (qualifierPartsToDrop > 0) {
            qualifierPartsToDrop--
            qualifiedAccess.explicitReceiver ?: qualifiedAccess
        } else {
            null
        }

    fun tryResolveAsQualifier(source: FirSourceElement?): FirResolvedQualifier? {
        if (qualifierStack.isEmpty()) {
            return null
        }
        val symbolProvider = session.firSymbolProvider
        var qualifierParts = qualifierStack.asReversed().map { it.asString() }
        var resolved: PackageOrClass?
        do {
            resolved = resolveToPackageOrClass(
                symbolProvider,
                FqName.fromSegments(qualifierParts)
            )
            if (resolved == null)
                qualifierParts = qualifierParts.dropLast(1)
        } while (resolved == null && qualifierParts.isNotEmpty())

        if (resolved != null) {
            qualifierPartsToDrop = qualifierParts.size - 1
            return FirResolvedQualifierImpl(
                source,
                resolved.packageFqName,
                resolved.relativeClassFqName
            ).apply { resultType = typeForQualifier(this) }
        }

        return null
    }

}