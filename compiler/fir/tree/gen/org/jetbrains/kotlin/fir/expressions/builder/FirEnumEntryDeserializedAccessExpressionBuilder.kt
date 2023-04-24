/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.fir.expressions.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirEnumEntryDeserializedAccessExpression
import org.jetbrains.kotlin.fir.expressions.builder.FirExpressionBuilder
import org.jetbrains.kotlin.fir.expressions.impl.FirEnumEntryDeserializedAccessExpressionImpl
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.constructClassType
import org.jetbrains.kotlin.fir.types.toLookupTag
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirEnumEntryDeserializedAccessExpressionBuilder : FirAnnotationContainerBuilder, FirExpressionBuilder {
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    lateinit var enumClassId: ClassId
    lateinit var enumEntryName: Name

    override fun build(): FirEnumEntryDeserializedAccessExpression {
        return FirEnumEntryDeserializedAccessExpressionImpl(
            annotations.toMutableOrEmpty(),
            enumClassId,
            enumEntryName,
        )
    }

    @Deprecated("Modification of 'source' has no impact for FirEnumEntryDeserializedAccessExpressionBuilder", level = DeprecationLevel.HIDDEN)
    override var source: KtSourceElement?
        get() = throw IllegalStateException()
        set(_) {
            throw IllegalStateException()
        }

    @Deprecated("Modification of 'typeRef' has no impact for FirEnumEntryDeserializedAccessExpressionBuilder", level = DeprecationLevel.HIDDEN)
    override var typeRef: FirTypeRef
        get() = throw IllegalStateException()
        set(_) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun buildEnumEntryDeserializedAccessExpression(init: FirEnumEntryDeserializedAccessExpressionBuilder.() -> Unit): FirEnumEntryDeserializedAccessExpression {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirEnumEntryDeserializedAccessExpressionBuilder().apply(init).build()
}
