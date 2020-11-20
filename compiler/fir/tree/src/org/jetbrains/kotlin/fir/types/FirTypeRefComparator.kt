/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.types.impl.FirImplicitBuiltinTypeRef

object FirTypeRefComparator : Comparator<FirTypeRef> {
    private val FirTypeRef.priority : Int
        get() = when (this) {
            is FirUserTypeRef -> 3
            is FirImplicitBuiltinTypeRef -> 2
            is FirResolvedTypeRef -> 1
            else -> 0
        }

    override fun compare(a: FirTypeRef, b: FirTypeRef): Int {
        val priorityDiff = a.priority - b.priority
        if (priorityDiff != 0) {
            return priorityDiff
        }

        when (a) {
            is FirUserTypeRef -> {
                require(b is FirUserTypeRef) {
                    "priority is inconsistent: ${a.render()} v.s. ${b.render()}"
                }
                val qualifierSizeDiff = a.qualifier.size - b.qualifier.size
                if (qualifierSizeDiff != 0) {
                    return qualifierSizeDiff
                }
                for ((aQualifier, bQualifier) in a.qualifier.zip(b.qualifier)) {
                    val qualifierNameDiff = aQualifier.name.compareTo(bQualifier.name)
                    if (qualifierNameDiff != 0) {
                        return qualifierNameDiff
                    }
                    val typeArgumentSizeDiff =
                        aQualifier.typeArgumentList.typeArguments.size - bQualifier.typeArgumentList.typeArguments.size
                    if (typeArgumentSizeDiff != 0) {
                        return typeArgumentSizeDiff
                    }
                    val typeArguments = aQualifier.typeArgumentList.typeArguments.zip(bQualifier.typeArgumentList.typeArguments)
                    for ((aTypeArgument, bTypeArgument) in typeArguments) {
                        val typeArgumentDiff = FirTypeProjectionComparator.compare(aTypeArgument, bTypeArgument)
                        if (typeArgumentDiff != 0) {
                            return typeArgumentDiff
                        }
                    }
                }
                return 0
            }
            is FirImplicitBuiltinTypeRef -> {
                require(b is FirImplicitBuiltinTypeRef) {
                    "priority is inconsistent: ${a.render()} v.s. ${b.render()}"
                }
                return a.id.shortClassName.compareTo(b.id.shortClassName)
            }
            is FirResolvedTypeRef -> {
                require(b is FirResolvedTypeRef) {
                    "priority is inconsistent: ${a.render()} v.s. ${b.render()}"
                }
                return ConeKotlinTypeComparator.compare(a.type, b.type)
            }
            else ->
                error("Unsupported type reference comparison: ${a.render()} v.s. ${b.render()}")
        }
    }
}
