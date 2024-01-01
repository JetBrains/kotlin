/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.fir.declarations.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.KtSourceFileLinesMapping
import org.jetbrains.kotlin.fir.FirFileAnnotationsContainer
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirPackageDirective
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirFileSymbol
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.fir.visitors.transformInplace

@OptIn(ResolveStateAccess::class)
internal class FirFileImpl(
    override val source: KtSourceElement?,
    resolvePhase: FirResolvePhase,
    override val moduleData: FirModuleData,
    override val origin: FirDeclarationOrigin,
    override val attributes: FirDeclarationAttributes,
    override var annotationsContainer: FirFileAnnotationsContainer?,
    override var packageDirective: FirPackageDirective,
    override val imports: MutableList<FirImport>,
    override val declarations: MutableList<FirDeclaration>,
    override val name: String,
    override val sourceFile: KtSourceFile?,
    override val sourceFileLinesMapping: KtSourceFileLinesMapping?,
    override val symbol: FirFileSymbol,
) : FirFile() {
    override val annotations: List<FirAnnotation>
        get() = annotationsContainer?.annotations ?: emptyList()
    override var controlFlowGraphReference: FirControlFlowGraphReference? = null

    init {
        symbol.bind(this)
        resolveState = resolvePhase.asResolveState()
    }

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        controlFlowGraphReference?.accept(visitor, data)
        annotationsContainer?.accept(visitor, data)
        packageDirective.accept(visitor, data)
        imports.forEach { it.accept(visitor, data) }
        declarations.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirFileImpl {
        controlFlowGraphReference = controlFlowGraphReference?.transform(transformer, data)
        transformAnnotationsContainer(transformer, data)
        packageDirective = packageDirective.transform(transformer, data)
        transformImports(transformer, data)
        transformDeclarations(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirFileImpl {
        return this
    }

    override fun <D> transformAnnotationsContainer(transformer: FirTransformer<D>, data: D): FirFileImpl {
        annotationsContainer = annotationsContainer?.transform(transformer, data)
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

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {}

    override fun replaceControlFlowGraphReference(newControlFlowGraphReference: FirControlFlowGraphReference?) {
        controlFlowGraphReference = newControlFlowGraphReference
    }
}
