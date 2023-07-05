/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.fir.declarations.builder

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.declarations.DeprecationsProvider
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationAttributes
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.builder.FirDeclarationBuilder
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
interface FirClassBuilder : FirDeclarationBuilder, FirAnnotationContainerBuilder {
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
