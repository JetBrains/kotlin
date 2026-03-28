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
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirAnonymousObjectImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousObjectSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef

@FirBuilderDsl
class FirAnonymousObjectBuilder : FirDeclarationBuilder, FirClassBuilder, FirAnnotationContainerBuilder {
    override var source: KtSourceElement? = null
    override var resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR
    override lateinit var moduleData: FirModuleData
    override lateinit var origin: FirDeclarationOrigin
    override var attributes: FirDeclarationAttributes = FirDeclarationAttributes()
    override val typeParameters: MutableList<FirTypeParameterRef> = mutableListOf()
    override lateinit var status: FirDeclarationStatus
    override var deprecationsProvider: DeprecationsProvider = UnresolvedDeprecationProvider
    override lateinit var scopeProvider: FirScopeProvider
    override lateinit var classKind: ClassKind
    override val superTypeRefs: MutableList<FirTypeRef> = mutableListOf()
    override val declarations: MutableList<FirDeclaration> = mutableListOf()
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    lateinit var symbol: FirAnonymousObjectSymbol

    override fun build(): FirAnonymousObject {
        return FirAnonymousObjectImpl(
            source,
            resolvePhase,
            moduleData,
            origin,
            attributes,
            typeParameters,
            status,
            deprecationsProvider,
            scopeProvider,
            classKind,
            superTypeRefs,
            declarations,
            annotations.toMutableOrEmpty(),
            symbol,
        )
    }


    @Deprecated("Modification of 'isLocal' has no impact for FirAnonymousObjectBuilder", level = DeprecationLevel.HIDDEN)
    override var isLocal: Boolean
        get() = throw IllegalStateException()
        set(_) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun buildAnonymousObject(init: FirAnonymousObjectBuilder.() -> Unit): FirAnonymousObject {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirAnonymousObjectBuilder().apply(init).build()
}

@OptIn(FirImplementationDetail::class)
fun buildAnonymousObject(
    source: KtSourceElement? = null,
    resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR,
    moduleData: FirModuleData,
    origin: FirDeclarationOrigin,
    attributes: FirDeclarationAttributes = FirDeclarationAttributes(),
    typeParameters: MutableList<FirTypeParameterRef> = mutableListOf(),
    status: FirDeclarationStatus,
    deprecationsProvider: DeprecationsProvider = UnresolvedDeprecationProvider,
    scopeProvider: FirScopeProvider,
    classKind: ClassKind,
    superTypeRefs: MutableList<FirTypeRef> = mutableListOf(),
    declarations: MutableList<FirDeclaration> = mutableListOf(),
    annotations: MutableList<FirAnnotation> = mutableListOf(),
    symbol: FirAnonymousObjectSymbol,
): FirAnonymousObject {
    return FirAnonymousObjectImpl(
        source,
        resolvePhase,
        moduleData,
        origin,
        attributes,
        typeParameters,
        status,
        deprecationsProvider,
        scopeProvider,
        classKind,
        superTypeRefs,
        declarations,
        annotations.toMutableOrEmpty(),
        symbol,
    )
}
