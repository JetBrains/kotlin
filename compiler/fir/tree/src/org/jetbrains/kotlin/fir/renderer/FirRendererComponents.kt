/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.renderer

import org.jetbrains.kotlin.fir.contracts.description.ConeContractRenderer

internal interface FirRendererComponents {
    val visitor: FirRenderer.Visitor
    val printer: FirPrinter
    val declarationRenderer: FirDeclarationRenderer?
    val annotationRenderer: FirAnnotationRenderer?
    val bodyRenderer: FirBodyRenderer?
    val callArgumentsRenderer: FirCallArgumentsRenderer?
    val classMemberRenderer: FirClassMemberRenderer?
    val contractRenderer: ConeContractRenderer?
    val idRenderer: ConeIdRenderer
    val modifierRenderer: FirModifierRenderer?
    val packageDirectiveRenderer: FirPackageDirectiveRenderer?
    val propertyAccessorRenderer: FirPropertyAccessorRenderer?
    val resolvePhaseRenderer: FirResolvePhaseRenderer?
    val typeRenderer: ConeTypeRenderer
    val referencedSymbolRenderer: FirSymbolRenderer
    val valueParameterRenderer: FirValueParameterRenderer?
    val errorExpressionRenderer: FirErrorExpressionRenderer?
    val fileAnnotationsContainerRenderer: FirFileAnnotationsContainerRenderer?
}