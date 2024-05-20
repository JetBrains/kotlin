/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.base

import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationsList
import org.jetbrains.kotlin.analysis.api.descriptors.annotations.KaFe10AnnotationsList
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaAnnotatedSymbol
import org.jetbrains.kotlin.descriptors.annotations.Annotations

internal interface KaFe10AnnotatedSymbol : KaAnnotatedSymbol, KaFe10Symbol {
    val annotationsObject: Annotations

    override val annotationsList: KaAnnotationsList
        get() = withValidityAssertion {
            KaFe10AnnotationsList.create(annotationsObject, analysisContext)
        }
}