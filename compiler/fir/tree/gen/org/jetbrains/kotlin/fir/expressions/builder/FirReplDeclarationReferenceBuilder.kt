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
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirReplDeclarationReference
import org.jetbrains.kotlin.fir.expressions.impl.FirReplDeclarationReferenceImpl
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol

@FirBuilderDsl
class FirReplDeclarationReferenceBuilder : FirAnnotationContainerBuilder {
    var source: KtSourceElement? = null
    lateinit var symbol: FirBasedSymbol<*>

    override fun build(): FirReplDeclarationReference {
        return FirReplDeclarationReferenceImpl(
            source,
            symbol,
        )
    }


    @Deprecated("Modification of 'annotations' has no impact for FirReplDeclarationReferenceBuilder", level = DeprecationLevel.HIDDEN)
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
}

@OptIn(ExperimentalContracts::class)
inline fun buildReplDeclarationReference(init: FirReplDeclarationReferenceBuilder.() -> Unit): FirReplDeclarationReference {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirReplDeclarationReferenceBuilder().apply(init).build()
}
