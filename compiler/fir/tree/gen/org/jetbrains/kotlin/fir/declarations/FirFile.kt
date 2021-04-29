/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirPureAbstractElement
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirFile : FirPureAbstractElement(), FirAnnotatedDeclaration {
    abstract override val source: FirSourceElement?
    abstract override val moduleData: FirModuleData
    abstract override val resolvePhase: FirResolvePhase
    abstract override val origin: FirDeclarationOrigin
    abstract override val attributes: FirDeclarationAttributes
    abstract override val annotations: List<FirAnnotationCall>
    abstract val imports: List<FirImport>
    abstract val declarations: List<FirDeclaration>
    abstract val name: String
    abstract val packageFqName: FqName

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitFile(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E: FirElement, D> transform(transformer: FirTransformer<D>, data: D): E = 
        transformer.transformFile(this, data) as E

    abstract override fun replaceResolvePhase(newResolvePhase: FirResolvePhase)

    abstract override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirFile

    abstract fun <D> transformImports(transformer: FirTransformer<D>, data: D): FirFile

    abstract fun <D> transformDeclarations(transformer: FirTransformer<D>, data: D): FirFile
}
