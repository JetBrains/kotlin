/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.impl

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.renderer.base.annotations.KaAnnotationRendererForSource
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KaDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KaRendererTypeApproximator
import org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.callables.KaSamConstructorSymbolRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.classifiers.KaSingleTypeParameterSymbolRenderer
import org.jetbrains.kotlin.analysis.api.renderer.types.impl.KaTypeRendererForDebug

@KaExperimentalApi
public object KaDeclarationRendererForDebug {
    public val WITH_QUALIFIED_NAMES: KaDeclarationRenderer = KaDeclarationRendererForSource.WITH_QUALIFIED_NAMES.with {
        singleTypeParameterRenderer = KaSingleTypeParameterSymbolRenderer.WITHOUT_BOUNDS
        samConstructorRenderer = KaSamConstructorSymbolRenderer.AS_FUNCTION
        typeRenderer = KaTypeRendererForDebug.WITH_QUALIFIED_NAMES
        declarationTypeApproximator = KaRendererTypeApproximator.NO_APPROXIMATION
    }

    public val WITH_QUALIFIED_NAMES_DENOTABLE: KaDeclarationRenderer = KaDeclarationRendererForSource.WITH_QUALIFIED_NAMES.with {
        singleTypeParameterRenderer = KaSingleTypeParameterSymbolRenderer.WITHOUT_BOUNDS
        samConstructorRenderer = KaSamConstructorSymbolRenderer.AS_FUNCTION
        typeRenderer = KaTypeRendererForDebug.WITH_QUALIFIED_NAMES
        declarationTypeApproximator = KaRendererTypeApproximator.TO_DENOTABLE
    }

    public val WITH_SHORT_NAMES: KaDeclarationRenderer = WITH_QUALIFIED_NAMES.with {
        annotationRenderer = KaAnnotationRendererForSource.WITH_SHORT_NAMES
        typeRenderer = KaTypeRendererForDebug.WITH_SHORT_NAMES
    }
}

@KaExperimentalApi
@Deprecated("Use 'KaDeclarationRendererForDebug' instead", ReplaceWith("KaDeclarationRendererForDebug"))
public typealias KtDeclarationRendererForDebug = KaDeclarationRendererForDebug
