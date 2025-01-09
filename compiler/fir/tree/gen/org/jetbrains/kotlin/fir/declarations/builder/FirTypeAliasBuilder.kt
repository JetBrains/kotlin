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
import org.jetbrains.kotlin.fir.declarations.impl.FirTypeAliasImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name

@FirBuilderDsl
class FirTypeAliasBuilder : FirDeclarationBuilder, FirTypeParameterRefsOwnerBuilder, FirAnnotationContainerBuilder {
    override var source: KtSourceElement? = null
    override var resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR
    override lateinit var moduleData: FirModuleData
    override lateinit var origin: FirDeclarationOrigin
    override var attributes: FirDeclarationAttributes = FirDeclarationAttributes()
    override val typeParameters: MutableList<FirTypeParameterRef> = mutableListOf()
    lateinit var status: FirDeclarationStatus
    var deprecationsProvider: DeprecationsProvider = UnresolvedDeprecationProvider
    lateinit var scopeProvider: FirScopeProvider
    lateinit var name: Name
    lateinit var symbol: FirTypeAliasSymbol
    lateinit var expandedTypeRef: FirTypeRef
    override val annotations: MutableList<FirAnnotation> = mutableListOf()

    override fun build(): FirTypeAlias {
        return FirTypeAliasImpl(
            source,
            resolvePhase,
            moduleData,
            origin,
            attributes,
            typeParameters,
            status,
            deprecationsProvider,
            scopeProvider,
            name,
            symbol,
            expandedTypeRef,
            annotations.toMutableOrEmpty(),
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildTypeAlias(init: FirTypeAliasBuilder.() -> Unit): FirTypeAlias {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirTypeAliasBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildTypeAliasCopy(original: FirTypeAlias, init: FirTypeAliasBuilder.() -> Unit): FirTypeAlias {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = FirTypeAliasBuilder()
    copyBuilder.source = original.source
    copyBuilder.resolvePhase = original.resolvePhase
    copyBuilder.moduleData = original.moduleData
    copyBuilder.origin = original.origin
    copyBuilder.attributes = original.attributes.copy()
    copyBuilder.typeParameters.addAll(original.typeParameters)
    copyBuilder.status = original.status
    copyBuilder.deprecationsProvider = original.deprecationsProvider
    copyBuilder.scopeProvider = original.scopeProvider
    copyBuilder.name = original.name
    copyBuilder.expandedTypeRef = original.expandedTypeRef
    copyBuilder.annotations.addAll(original.annotations)
    return copyBuilder.apply(init).build()
}
