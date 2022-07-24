/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.fir.declarations.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.KtSourceFileLinesMapping
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirPackageDirective
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationAttributes
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirImport
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.symbols.impl.FirFileSymbol
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class FirFileImpl(
    override val source: KtSourceElement?,
    override val annotations: MutableList<FirAnnotation>,
    override val moduleData: FirModuleData,
    @Volatile
    override var resolvePhase: FirResolvePhase,
    override val origin: FirDeclarationOrigin,
    override val attributes: FirDeclarationAttributes,
    override var packageDirective: FirPackageDirective,
    override val imports: MutableList<FirImport>,
    override val declarations: MutableList<FirDeclaration>,
    override val name: String,
    override val sourceFile: KtSourceFile?,
    override val sourceFileLinesMapping: KtSourceFileLinesMapping?,
) : FirFile() {
    override val symbol: FirFileSymbol = FirFileSymbol()

    init {
        symbol.bind(this)
    }

    override val elementKind get() = FirElementKind.File

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        annotations.clear()
        annotations.addAll(newAnnotations)
    }

    override fun replaceResolvePhase(newResolvePhase: FirResolvePhase) {
        resolvePhase = newResolvePhase
    }

    override fun replacePackageDirective(newPackageDirective: FirPackageDirective) {
        packageDirective = newPackageDirective
    }

    override fun replaceImports(newImports: List<FirImport>) {
        imports.clear()
        imports.addAll(newImports)
    }

    override fun replaceDeclarations(newDeclarations: List<FirDeclaration>) {
        declarations.clear()
        declarations.addAll(newDeclarations)
    }
}
