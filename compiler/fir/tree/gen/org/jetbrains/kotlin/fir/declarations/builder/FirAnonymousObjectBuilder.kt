/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.declarations.DeprecationsPerUseSite
import org.jetbrains.kotlin.fir.declarations.FirAnonymousObject
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationAttributes
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.builder.FirClassBuilder
import org.jetbrains.kotlin.fir.declarations.builder.FirDeclarationBuilder
import org.jetbrains.kotlin.fir.declarations.impl.FirAnonymousObjectImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousObjectSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirAnonymousObjectBuilder : FirDeclarationBuilder, FirClassBuilder, FirAnnotationContainerBuilder {
    override var source: KtSourceElement? = null
    override lateinit var moduleData: FirModuleData
    override var resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR
    override lateinit var origin: FirDeclarationOrigin
    override var attributes: FirDeclarationAttributes = FirDeclarationAttributes()
    override val typeParameters: MutableList<FirTypeParameterRef> = mutableListOf()
    override lateinit var status: FirDeclarationStatus
    override var deprecation: DeprecationsPerUseSite? = null
    override lateinit var classKind: ClassKind
    override val superTypeRefs: MutableList<FirTypeRef> = mutableListOf()
    override val declarations: MutableList<FirDeclaration> = mutableListOf()
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    override lateinit var scopeProvider: FirScopeProvider
    lateinit var symbol: FirAnonymousObjectSymbol

    override fun build(): FirAnonymousObject {
        return FirAnonymousObjectImpl(
            source,
            moduleData,
            resolvePhase,
            origin,
            attributes,
            typeParameters,
            status,
            deprecation,
            classKind,
            superTypeRefs,
            declarations,
            annotations,
            scopeProvider,
            symbol,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildAnonymousObject(init: FirAnonymousObjectBuilder.() -> Unit): FirAnonymousObject {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirAnonymousObjectBuilder().apply(init).build()
}
