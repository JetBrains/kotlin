/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.annotations.*
import org.jetbrains.kotlin.analysis.api.symbols.KtClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtAnnotatedSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.light.classes.symbol.withSymbol
import org.jetbrains.kotlin.name.ClassId

internal class SymbolAnnotationsProvider<T : KtAnnotatedSymbol>(
    private val ktModule: KtModule,
    private val annotatedSymbolPointer: KtSymbolPointer<T>,
    private val annotationUseSiteTarget: AnnotationUseSiteTarget? = null,
    private val acceptAnnotationsWithoutSite: Boolean = false,
) : AnnotationsProvider {
    private inline fun <T> withAnnotatedSymbol(crossinline action: context(KtAnalysisSession) (KtAnnotatedSymbol) -> T): T =
        annotatedSymbolPointer.withSymbol(ktModule, action)

    override fun annotationOverviews(): List<KtAnnotationOverview> = withAnnotatedSymbol { annotatedSymbol ->
        annotatedSymbol.annotationOverviews.filter {
            it.useSiteTarget == annotationUseSiteTarget || acceptAnnotationsWithoutSite && it.useSiteTarget == null
        }
    }

    override fun get(classId: ClassId): Collection<KtAnnotationApplication> = withAnnotatedSymbol { annotatedSymbol ->
        annotatedSymbol.annotationsByClassId(classId).filter {
            it.useSiteTarget == annotationUseSiteTarget || acceptAnnotationsWithoutSite && it.useSiteTarget == null
        }
    }

    override fun contains(classId: ClassId): Boolean = withAnnotatedSymbol { annotatedSymbol ->
        annotatedSymbol.hasAnnotation(classId, annotationUseSiteTarget, acceptAnnotationsWithoutSite)
    }

    override fun isTheSameAs(other: Any?): Boolean = other === this ||
            other is SymbolAnnotationsProvider<*> &&
            other.ktModule == ktModule &&
            other.annotationUseSiteTarget == annotationUseSiteTarget &&
            other.acceptAnnotationsWithoutSite == acceptAnnotationsWithoutSite &&
            annotatedSymbolPointer.pointsToTheSameSymbolAs(other.annotatedSymbolPointer)

    override fun ownerClassId(): ClassId? = withAnnotatedSymbol { annotatedSymbol ->
        (annotatedSymbol as? KtClassLikeSymbol)?.classIdIfNonLocal
    }
}
