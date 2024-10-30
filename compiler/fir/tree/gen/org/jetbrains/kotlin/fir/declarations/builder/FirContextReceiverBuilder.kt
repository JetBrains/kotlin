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
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirContextReceiverImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirReceiverParameterSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name

@FirBuilderDsl
class FirContextReceiverBuilder : FirAnnotationContainerBuilder {
    override var source: KtSourceElement? = null
    var resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    lateinit var moduleData: FirModuleData
    lateinit var origin: FirDeclarationOrigin
    var attributes: FirDeclarationAttributes = FirDeclarationAttributes()
    lateinit var symbol: FirReceiverParameterSymbol
    lateinit var returnTypeRef: FirTypeRef
    lateinit var containingDeclarationSymbol: FirBasedSymbol<*>
    var customLabelName: Name? = null
    var labelNameFromTypeRef: Name? = null

    override fun build(): FirContextReceiver {
        return FirContextReceiverImpl(
            source,
            resolvePhase,
            annotations.toMutableOrEmpty(),
            moduleData,
            origin,
            attributes,
            symbol,
            returnTypeRef,
            containingDeclarationSymbol,
            customLabelName,
            labelNameFromTypeRef,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildContextReceiver(init: FirContextReceiverBuilder.() -> Unit): FirContextReceiver {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirContextReceiverBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildContextReceiverCopy(original: FirContextReceiver, init: FirContextReceiverBuilder.() -> Unit): FirContextReceiver {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = FirContextReceiverBuilder()
    copyBuilder.source = original.source
    copyBuilder.resolvePhase = original.resolvePhase
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.moduleData = original.moduleData
    copyBuilder.origin = original.origin
    copyBuilder.attributes = original.attributes.copy()
    copyBuilder.returnTypeRef = original.returnTypeRef
    copyBuilder.containingDeclarationSymbol = original.containingDeclarationSymbol
    copyBuilder.customLabelName = original.customLabelName
    copyBuilder.labelNameFromTypeRef = original.labelNameFromTypeRef
    return copyBuilder.apply(init).build()
}
