/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.*
import org.jetbrains.kotlin.fir.types.impl.*

fun ModuleInfo.dependenciesWithoutSelf(): Sequence<ModuleInfo> = dependencies().asSequence().filter { it != this }

// TODO: rewrite
fun FirBlock.returnExpressions(): List<FirExpression> = listOfNotNull(statements.lastOrNull() as? FirExpression)

private val PUBLIC_METHOD_NAMES_IN_OBJECT = setOf("equals", "hashCode", "getClass", "wait", "notify", "notifyAll", "toString")

// do we need a deep copy here ?
fun <R : FirTypeRef> R.copyWithNewSourceKind(newKind: FirFakeSourceElementKind): R {
    if (source == null) return this
    if (source?.kind == newKind) return this
    val newSource = source?.fakeElement(newKind)

    @Suppress("UNCHECKED_CAST")
    return when (val typeRef = this) {
        is FirResolvedTypeRefImpl -> buildResolvedTypeRefCopy(typeRef) {
            source = newSource
        }
        is FirErrorTypeRef -> buildErrorTypeRefCopy(typeRef) {
            source = newSource
        }
        is FirUserTypeRefImpl -> buildUserTypeRef {
            source = newSource
            isMarkedNullable = typeRef.isMarkedNullable
            qualifier += typeRef.qualifier
            annotations += typeRef.annotations
        }
        is FirImplicitTypeRef -> buildImplicitTypeRefCopy(typeRef) {
            source = newSource
        }
        is FirComposedSuperTypeRef -> buildComposedSuperTypeRefCopy(typeRef) {
            source = newSource
        }
        is FirFunctionTypeRefImpl -> buildFunctionTypeRefCopy(typeRef) {
            source = newSource
        }
        is FirDynamicTypeRef -> buildDynamicTypeRef {
            source = newSource
            isMarkedNullable = typeRef.isMarkedNullable
            annotations += typeRef.annotations
        }
        is FirImplicitBuiltinTypeRef -> typeRef.withFakeSource(newKind)
        else -> TODO("Not implemented for ${typeRef::class}")
    } as R
}

