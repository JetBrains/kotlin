/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.fir.expressions.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.UnresolvedExpressionTypeAccess
import org.jetbrains.kotlin.fir.expressions.builder.FirAbstractResolvedQualifierBuilder
import org.jetbrains.kotlin.fir.expressions.builder.FirExpressionBuilder
import org.jetbrains.kotlin.fir.expressions.impl.FirResolvedQualifierImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirResolvedQualifierBuilder : FirAbstractResolvedQualifierBuilder, FirAnnotationContainerBuilder, FirExpressionBuilder {
    override var source: KtSourceElement? = null
    override var coneTypeOrNull: ConeKotlinType? = null
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    override lateinit var packageFqName: FqName
    override var relativeClassFqName: FqName? = null
    override var symbol: FirClassLikeSymbol<*>? = null
    override var isNullableLHSForCallableReference: Boolean = false
    override var canBeValue: Boolean = false
    override var isFullyQualified: Boolean = false
    override val nonFatalDiagnostics: MutableList<ConeDiagnostic> = mutableListOf()
    override val typeArguments: MutableList<FirTypeProjection> = mutableListOf()

    override fun build(): FirResolvedQualifier {
        return FirResolvedQualifierImpl(
            source,
            coneTypeOrNull,
            annotations.toMutableOrEmpty(),
            packageFqName,
            relativeClassFqName,
            symbol,
            isNullableLHSForCallableReference,
            canBeValue,
            isFullyQualified,
            nonFatalDiagnostics.toMutableOrEmpty(),
            typeArguments.toMutableOrEmpty(),
        )
    }


    @Deprecated("Modification of 'classId' has no impact for FirResolvedQualifierBuilder", level = DeprecationLevel.HIDDEN)
    override var classId: ClassId?
        get() = throw IllegalStateException()
        set(_) {
            throw IllegalStateException()
        }

    @Deprecated("Modification of 'resolvedToCompanionObject' has no impact for FirResolvedQualifierBuilder", level = DeprecationLevel.HIDDEN)
    override var resolvedToCompanionObject: Boolean
        get() = throw IllegalStateException()
        set(_) {
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
