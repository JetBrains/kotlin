/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.fir.expressions.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirEnumEntryDeserializedAccessExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirEnumEntryDeserializedAccessExpressionImpl
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

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

    @Deprecated("Modification of 'coneTypeOrNull' has no impact for FirEnumEntryDeserializedAccessExpressionBuilder", level = DeprecationLevel.HIDDEN)
    override var coneTypeOrNull: ConeKotlinType?
        get() = throw IllegalStateException()
        set(_) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun buildEnumEntryDeserializedAccessExpression(init: FirEnumEntryDeserializedAccessExpressionBuilder.() -> Unit): FirEnumEntryDeserializedAccessExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirEnumEntryDeserializedAccessExpressionBuilder().apply(init).build()
}
