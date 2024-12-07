/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.fir.declarations.builder

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.fir.types.FirTypeRef

@FirBuilderDsl
sealed interface FirClassBuilder : FirDeclarationBuilder, FirAnnotationContainerBuilder {
    abstract override var source: KtSourceElement?
    abstract override var resolvePhase: FirResolvePhase
    abstract override val annotations: MutableList<FirAnnotation>
    abstract override var moduleData: FirModuleData
    abstract override var origin: FirDeclarationOrigin
    abstract override var attributes: FirDeclarationAttributes
    abstract val typeParameters: MutableList<FirTypeParameterRef>
    abstract var status: FirDeclarationStatus
    abstract var deprecationsProvider: DeprecationsProvider
    abstract var classKind: ClassKind
    abstract val superTypeRefs: MutableList<FirTypeRef>
    abstract val declarations: MutableList<FirDeclaration>
    abstract var scopeProvider: FirScopeProvider
    override fun build(): FirClass
}
