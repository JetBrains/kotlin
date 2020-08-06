/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.contracts.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.contracts.FirEffectDeclaration
import org.jetbrains.kotlin.fir.contracts.description.ConeEffectDeclaration
import org.jetbrains.kotlin.fir.contracts.impl.FirEffectDeclarationImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.builder.FirExpressionBuilder
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImpl
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirEffectDeclarationBuilder : FirAnnotationContainerBuilder, FirExpressionBuilder {
    override var source: FirSourceElement? = null
    override val annotations: MutableList<FirAnnotationCall> = mutableListOf()
    lateinit var effect: ConeEffectDeclaration

    override fun build(): FirEffectDeclaration {
        return FirEffectDeclarationImpl(
            source,
            annotations,
            effect,
        )
    }


    @Deprecated("Modification of 'typeRef' has no impact for FirEffectDeclarationBuilder", level = DeprecationLevel.HIDDEN)
    override var typeRef: FirTypeRef
        get() = throw IllegalStateException()
        set(value) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun buildEffectDeclaration(init: FirEffectDeclarationBuilder.() -> Unit): FirEffectDeclaration {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirEffectDeclarationBuilder().apply(init).build()
}
