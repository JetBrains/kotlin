/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccess
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.builder.buildResolvedQualifier
import org.jetbrains.kotlin.fir.references.impl.FirSimpleNamedReference
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.resolve.transformers.PackageOrClass
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.resultType
import org.jetbrains.kotlin.fir.resolve.transformers.resolveToPackageOrClass
import org.jetbrains.kotlin.fir.resolve.typeForQualifier
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class FirQualifiedNameResolver(private val components: BodyResolveComponents) {
    private val session = components.session
    private var qualifierStack = mutableListOf<NameWithTypeArguments>()
    private var qualifierPartsToDrop = 0

    private class NameWithTypeArguments(val name: Name, val typeArguments: List<FirTypeProjection>)

    fun reset() {
        qualifierStack.clear()
        qualifierPartsToDrop = 0
    }


    /**
     * NB: 0 if current 'qualifiedAccess.safe || callee.name.isSpecial', 1 if current is fine, 2 if potential qualifier
     * a.b.c
     *   ^ here stack will be ['c', 'b'], so possible
     * a.b?.c
     *   ^ here stack will be ['b'], so impossible
     * a?.b.c
     *    ^ here stack will be [], so impossible
     */
    fun isPotentialQualifierPartPosition() = qualifierStack.size > 1

    fun initProcessingQualifiedAccess(callee: FirSimpleNamedReference, typeArguments: List<FirTypeProjection>) {
        if (callee.name.isSpecial) {
            qualifierStack.clear()
        } else {
            qualifierStack.add(NameWithTypeArguments(callee.name, typeArguments.toList()))
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
        var qualifierParts = qualifierStack.asReversed().map { it.name.asString() }
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
            return buildResolvedQualifier {
                this.source = source
                packageFqName = resolved.packageFqName
                relativeClassFqName = resolved.relativeClassFqName
                symbol = resolved.classSymbol
                typeArguments.addAll(qualifierStack.take(qualifierParts.size).flatMap { it.typeArguments })
            }.apply {
                resultType = components.typeForQualifier(this)
            }
        }

        return null
    }

}
