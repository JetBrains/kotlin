/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.builder

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.builder.FirClassBuilder
import org.jetbrains.kotlin.fir.declarations.builder.FirTypeParametersOwnerBuilder
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.name.Name

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
interface AbstractFirRegularClassBuilder : FirClassBuilder, FirTypeParametersOwnerBuilder {
    abstract override var source: FirSourceElement?
    abstract override val annotations: MutableList<FirAnnotationCall>
    abstract override var session: FirSession
    abstract override var classKind: ClassKind
    abstract override val superTypeRefs: MutableList<FirTypeRef>
    abstract override val declarations: MutableList<FirDeclaration>
    abstract override var scopeProvider: FirScopeProvider
    abstract override val typeParameters: MutableList<FirTypeParameter>
    abstract var resolvePhase: FirResolvePhase
    abstract var status: FirDeclarationStatus
    abstract var name: Name
    abstract var symbol: FirRegularClassSymbol
    abstract var companionObject: FirRegularClass?
    abstract var hasLazyNestedClassifiers: Boolean
    override fun build(): FirRegularClass
}
