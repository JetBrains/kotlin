/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.renderer

internal interface FirRendererComponents {
    val visitor: FirRenderer.Visitor
    val printer: FirPrinter
    val declarationRenderer: FirDeclarationRenderer
    val annotationRenderer: FirAnnotationRenderer?
    val bodyRenderer: FirBodyRenderer?
    val packageDirectiveRenderer: FirPackageDirectiveRenderer?
    val typeRenderer: ConeTypeRenderer
}