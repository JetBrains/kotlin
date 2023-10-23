/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.impl

import org.jetbrains.kotlin.analysis.api.renderer.declarations.KtDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KtRendererTypeApproximator
import org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.callables.KtSamConstructorSymbolRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.classifiers.KtSingleTypeParameterSymbolRenderer
import org.jetbrains.kotlin.analysis.api.renderer.types.impl.KtTypeRendererForDebug

public object KtDeclarationRendererForDebug {
    public val WITH_QUALIFIED_NAMES: KtDeclarationRenderer = KtDeclarationRendererForSource.WITH_QUALIFIED_NAMES.with {
        singleTypeParameterRenderer = KtSingleTypeParameterSymbolRenderer.WITHOUT_BOUNDS
        samConstructorRenderer = KtSamConstructorSymbolRenderer.AS_FUNCTION
        typeRenderer = KtTypeRendererForDebug.WITH_QUALIFIED_NAMES
        declarationTypeApproximator = KtRendererTypeApproximator.NO_APPROXIMATION
    }

    public val WITH_QUALIFIED_NAMES_DENOTABLE: KtDeclarationRenderer = KtDeclarationRendererForSource.WITH_QUALIFIED_NAMES.with {
        singleTypeParameterRenderer = KtSingleTypeParameterSymbolRenderer.WITHOUT_BOUNDS
        samConstructorRenderer = KtSamConstructorSymbolRenderer.AS_FUNCTION
        typeRenderer = KtTypeRendererForDebug.WITH_QUALIFIED_NAMES
        declarationTypeApproximator = KtRendererTypeApproximator.TO_DENOTABLE
    }
}

