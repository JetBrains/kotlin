/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.fir.expressions.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.expressions.UnresolvedExpressionTypeAccess
import org.jetbrains.kotlin.fir.expressions.builder.FirExpressionBuilder
import org.jetbrains.kotlin.fir.expressions.impl.FirAnnotationImpl
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneTypeOrNull
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirAnnotationBuilder : FirAnnotationContainerBuilder, FirExpressionBuilder {
    override var source: KtSourceElement? = null
    var useSiteTarget: AnnotationUseSiteTarget? = null
    lateinit var annotationTypeRef: FirTypeRef
    lateinit var argumentMapping: FirAnnotationArgumentMapping
    val typeArguments: MutableList<FirTypeProjection> = mutableListOf()

    override fun build(): FirAnnotation {
        return FirAnnotationImpl(
            source,
            useSiteTarget,
            annotationTypeRef,
            argumentMapping,
            typeArguments.toMutableOrEmpty(),
        )
    }


    @Deprecated("Modification of 'coneTypeOrNull' has no impact for FirAnnotationBuilder", level = DeprecationLevel.HIDDEN)
    override var coneTypeOrNull: ConeKotlinType?
        get() = throw IllegalStateException()
        set(_) {
            throw IllegalStateException()
        }

    @Deprecated("Modification of 'annotations' has no impact for FirAnnotationBuilder", level = DeprecationLevel.HIDDEN)
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
}

@OptIn(ExperimentalContracts::class)
inline fun buildAnnotation(init: FirAnnotationBuilder.() -> Unit): FirAnnotation {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirAnnotationBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildAnnotationCopy(original: FirAnnotation, init: FirAnnotationBuilder.() -> Unit): FirAnnotation {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = FirAnnotationBuilder()
    copyBuilder.source = original.source
    copyBuilder.useSiteTarget = original.useSiteTarget
    copyBuilder.annotationTypeRef = original.annotationTypeRef
    copyBuilder.argumentMapping = original.argumentMapping
    copyBuilder.typeArguments.addAll(original.typeArguments)
    return copyBuilder.apply(init).build()
}
