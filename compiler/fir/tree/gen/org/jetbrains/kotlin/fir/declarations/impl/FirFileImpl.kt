/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationAttributes
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirImport
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class FirFileImpl(
    override val source: FirSourceElement?,
    override val moduleData: FirModuleData,
    override var resolvePhase: FirResolvePhase,
    override val origin: FirDeclarationOrigin,
    override val attributes: FirDeclarationAttributes,
    override val annotations: MutableList<FirAnnotationCall>,
    override val imports: MutableList<FirImport>,
    override val declarations: MutableList<FirDeclaration>,
    override val name: String,
    override val packageFqName: FqName,
) : FirFile() {
    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        imports.forEach { it.accept(visitor, data) }
        declarations.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirFileImpl {
        transformAnnotations(transformer, data)
        transformImports(transformer, data)
        transformDeclarations(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirFileImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformImports(transformer: FirTransformer<D>, data: D): FirFileImpl {
        imports.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformDeclarations(transformer: FirTransformer<D>, data: D): FirFileImpl {
        declarations.transformInplace(transformer, data)
        return this
    }

    override fun replaceResolvePhase(newResolvePhase: FirResolvePhase) {
        resolvePhase = newResolvePhase
    }
}
