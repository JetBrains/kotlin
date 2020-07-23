/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirErrorResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.builder.FirAbstractResolvedQualifierBuilder
import org.jetbrains.kotlin.fir.expressions.builder.FirExpressionBuilder
import org.jetbrains.kotlin.fir.expressions.impl.FirErrorResolvedQualifierImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImpl
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirErrorResolvedQualifierBuilder : FirAbstractResolvedQualifierBuilder, FirAnnotationContainerBuilder, FirExpressionBuilder {
    override var source: FirSourceElement? = null
    override val annotations: MutableList<FirAnnotationCall> = mutableListOf()
    override lateinit var packageFqName: FqName
    override var relativeClassFqName: FqName? = null
    override var classId: ClassId? = null
    override var symbol: FirClassLikeSymbol<*>? = null
    override var isNullableLHSForCallableReference: Boolean = false
    override val typeArguments: MutableList<FirTypeProjection> = mutableListOf()
    lateinit var diagnostic: ConeDiagnostic

    override fun build(): FirErrorResolvedQualifier {
        return FirErrorResolvedQualifierImpl(
            source,
            annotations,
            packageFqName,
            relativeClassFqName,
            classId,
            symbol,
            isNullableLHSForCallableReference,
            typeArguments,
            diagnostic,
        )
    }


    @Deprecated("Modification of 'typeRef' has no impact for FirErrorResolvedQualifierBuilder", level = DeprecationLevel.HIDDEN)
    override var typeRef: FirTypeRef
        get() = throw IllegalStateException()
        set(value) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun buildErrorResolvedQualifier(init: FirErrorResolvedQualifierBuilder.() -> Unit): FirErrorResolvedQualifier {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirErrorResolvedQualifierBuilder().apply(init).build()
}
