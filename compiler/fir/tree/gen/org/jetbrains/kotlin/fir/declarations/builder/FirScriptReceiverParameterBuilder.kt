/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.fir.declarations.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.declarations.FirScriptReceiverParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirScriptReceiverParameterImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.types.FirTypeRef

@FirBuilderDsl
class FirScriptReceiverParameterBuilder : FirAnnotationContainerBuilder {
    override var source: KtSourceElement? = null
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    lateinit var typeRef: FirTypeRef
    var isBaseClassReceiver: Boolean by kotlin.properties.Delegates.notNull<Boolean>()

    override fun build(): FirScriptReceiverParameter {
        return FirScriptReceiverParameterImpl(
            source,
            annotations.toMutableOrEmpty(),
            typeRef,
            isBaseClassReceiver,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildScriptReceiverParameter(init: FirScriptReceiverParameterBuilder.() -> Unit): FirScriptReceiverParameter {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirScriptReceiverParameterBuilder().apply(init).build()
}
