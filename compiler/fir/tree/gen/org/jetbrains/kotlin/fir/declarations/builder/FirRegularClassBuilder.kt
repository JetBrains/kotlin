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
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirRegularClassImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name

@FirBuilderDsl
open class FirRegularClassBuilder : FirClassBuilder, FirTypeParameterRefsOwnerBuilder, FirAnnotationContainerBuilder {
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
    override val declarations: MutableList<FirDeclaration> = mutableListOf()
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    open lateinit var name: Name
    open lateinit var symbol: FirRegularClassSymbol
    open var companionObjectSymbol: FirRegularClassSymbol? = null
    override val superTypeRefs: MutableList<FirTypeRef> = mutableListOf()
    open val contextParameters: MutableList<FirValueParameter> = mutableListOf()

    override fun build(): FirRegularClass {
        return FirRegularClassImpl(
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
            declarations,
            annotations.toMutableOrEmpty(),
            name,
            symbol,
            companionObjectSymbol,
            superTypeRefs,
            contextParameters.toMutableOrEmpty(),
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildRegularClass(init: FirRegularClassBuilder.() -> Unit): FirRegularClass {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirRegularClassBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class, DirectDeclarationsAccess::class)
inline fun buildRegularClassCopy(original: FirRegularClass, init: FirRegularClassBuilder.() -> Unit): FirRegularClass {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = FirRegularClassBuilder()
    copyBuilder.source = original.source
    copyBuilder.resolvePhase = original.resolvePhase
    copyBuilder.moduleData = original.moduleData
    copyBuilder.origin = original.origin
    copyBuilder.attributes = original.attributes.copy()
    copyBuilder.typeParameters.addAll(original.typeParameters)
    copyBuilder.status = original.status
    copyBuilder.deprecationsProvider = original.deprecationsProvider
    copyBuilder.scopeProvider = original.scopeProvider
    copyBuilder.classKind = original.classKind
    copyBuilder.declarations.addAll(original.declarations)
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.name = original.name
    copyBuilder.companionObjectSymbol = original.companionObjectSymbol
    copyBuilder.superTypeRefs.addAll(original.superTypeRefs)
    copyBuilder.contextParameters.addAll(original.contextParameters)
    return copyBuilder.apply(init).build()
}
