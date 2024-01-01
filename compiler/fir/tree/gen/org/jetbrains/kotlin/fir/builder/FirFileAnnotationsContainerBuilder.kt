/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.fir.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirFileAnnotationsContainer
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.impl.FirFileAnnotationsContainerImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirFileSymbol

@FirBuilderDsl
class FirFileAnnotationsContainerBuilder : FirAnnotationContainerBuilder {
    override var source: KtSourceElement? = null
    var resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR
    lateinit var moduleData: FirModuleData
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    lateinit var containingFileSymbol: FirFileSymbol

    override fun build(): FirFileAnnotationsContainer {
        return FirFileAnnotationsContainerImpl(
            source,
            resolvePhase,
            moduleData,
            annotations.toMutableOrEmpty(),
            containingFileSymbol,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildFileAnnotationsContainer(init: FirFileAnnotationsContainerBuilder.() -> Unit): FirFileAnnotationsContainer {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirFileAnnotationsContainerBuilder().apply(init).build()
}
