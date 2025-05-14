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
import org.jetbrains.kotlin.fir.declarations.impl.FirEnumEntryImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.symbols.impl.FirEnumEntrySymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name

@FirBuilderDsl
class FirEnumEntryBuilder : FirAnnotationContainerBuilder {
    var source: KtSourceElement? = null
    var resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR
    lateinit var moduleData: FirModuleData
    lateinit var origin: FirDeclarationOrigin
    var attributes: FirDeclarationAttributes = FirDeclarationAttributes()
    lateinit var status: FirDeclarationStatus
    lateinit var returnTypeRef: FirTypeRef
    var staticReceiverParameter: FirTypeRef? = null
    var deprecationsProvider: DeprecationsProvider = UnresolvedDeprecationProvider
    lateinit var name: Name
    var initializer: FirExpression? = null
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    lateinit var symbol: FirEnumEntrySymbol

    override fun build(): FirEnumEntry {
        return FirEnumEntryImpl(
            source,
            resolvePhase,
            moduleData,
            origin,
            attributes,
            status,
            returnTypeRef,
            staticReceiverParameter,
            deprecationsProvider,
            name,
            initializer,
            annotations.toMutableOrEmpty(),
            symbol,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildEnumEntry(init: FirEnumEntryBuilder.() -> Unit): FirEnumEntry {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirEnumEntryBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildEnumEntryCopy(original: FirEnumEntry, init: FirEnumEntryBuilder.() -> Unit): FirEnumEntry {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = FirEnumEntryBuilder()
    copyBuilder.source = original.source
    copyBuilder.resolvePhase = original.resolvePhase
    copyBuilder.moduleData = original.moduleData
    copyBuilder.origin = original.origin
    copyBuilder.attributes = original.attributes.copy()
    copyBuilder.status = original.status
    copyBuilder.returnTypeRef = original.returnTypeRef
    copyBuilder.staticReceiverParameter = original.staticReceiverParameter
    copyBuilder.deprecationsProvider = original.deprecationsProvider
    copyBuilder.name = original.name
    copyBuilder.initializer = original.initializer
    copyBuilder.annotations.addAll(original.annotations)
    return copyBuilder.apply(init).build()
}
