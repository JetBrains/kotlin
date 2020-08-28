/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.builder.FirAbstractResolvedQualifierBuilder
import org.jetbrains.kotlin.fir.expressions.builder.FirExpressionBuilder
import org.jetbrains.kotlin.fir.expressions.impl.FirResolvedQualifierImpl
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
class FirResolvedQualifierBuilder : FirAbstractResolvedQualifierBuilder, FirAnnotationContainerBuilder, FirExpressionBuilder {
    override var source: FirSourceElement? = null
    override var typeRef: FirTypeRef = FirImplicitTypeRefImpl(null)
    override val annotations: MutableList<FirAnnotationCall> = mutableListOf()
    override lateinit var packageFqName: FqName
    override var relativeClassFqName: FqName? = null
    override var symbol: FirClassLikeSymbol<*>? = null
    override var isNullableLHSForCallableReference: Boolean = false
    override val typeArguments: MutableList<FirTypeProjection> = mutableListOf()

    override fun build(): FirResolvedQualifier {
        return FirResolvedQualifierImpl(
            source,
            typeRef,
            annotations,
            packageFqName,
            relativeClassFqName,
            symbol,
            isNullableLHSForCallableReference,
            typeArguments,
        )
    }


    @Deprecated("Modification of 'classId' has no impact for FirResolvedQualifierBuilder", level = DeprecationLevel.HIDDEN)
    override var classId: ClassId?
        get() = throw IllegalStateException()
        set(value) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun buildResolvedQualifier(init: FirResolvedQualifierBuilder.() -> Unit): FirResolvedQualifier {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirResolvedQualifierBuilder().apply(init).build()
}
