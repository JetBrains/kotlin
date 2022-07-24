/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.KtSourceFileLinesMapping
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirPackageDirective
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.symbols.impl.FirFileSymbol
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirFile : FirDeclaration() {
    abstract override val source: KtSourceElement?
    abstract override val annotations: List<FirAnnotation>
    abstract override val moduleData: FirModuleData
    abstract override val resolvePhase: FirResolvePhase
    abstract override val origin: FirDeclarationOrigin
    abstract override val attributes: FirDeclarationAttributes
    abstract val packageDirective: FirPackageDirective
    abstract val imports: List<FirImport>
    abstract val declarations: List<FirDeclaration>
    abstract val name: String
    abstract val sourceFile: KtSourceFile?
    abstract val sourceFileLinesMapping: KtSourceFileLinesMapping?
    abstract override val symbol: FirFileSymbol


    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract override fun replaceResolvePhase(newResolvePhase: FirResolvePhase)

    abstract fun replacePackageDirective(newPackageDirective: FirPackageDirective)

    abstract fun replaceImports(newImports: List<FirImport>)

    abstract fun replaceDeclarations(newDeclarations: List<FirDeclaration>)
}

inline fun <D> FirFile.transformAnnotations(transformer: FirTransformer<D>, data: D): FirFile 
     = apply { replaceAnnotations(annotations.transform(transformer, data)) }

inline fun <D> FirFile.transformPackageDirective(transformer: FirTransformer<D>, data: D): FirFile 
     = apply { replacePackageDirective(packageDirective.transform(transformer, data)) }

inline fun <D> FirFile.transformImports(transformer: FirTransformer<D>, data: D): FirFile 
     = apply { replaceImports(imports.transform(transformer, data)) }

inline fun <D> FirFile.transformDeclarations(transformer: FirTransformer<D>, data: D): FirFile 
     = apply { replaceDeclarations(declarations.transform(transformer, data)) }
