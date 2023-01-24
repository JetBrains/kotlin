/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationApplication
import org.jetbrains.kotlin.analysis.api.annotations.annotationClassIds
import org.jetbrains.kotlin.analysis.api.annotations.annotationsByClassId
import org.jetbrains.kotlin.analysis.api.annotations.hasAnnotation
import org.jetbrains.kotlin.analysis.api.symbols.KtClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtAnnotatedSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.light.classes.symbol.withSymbol
import org.jetbrains.kotlin.name.ClassId

internal class SymbolAnnotationsProvider<T : KtAnnotatedSymbol>(
    private val ktModule: KtModule,
    private val annotatedSymbolPointer: KtSymbolPointer<T>
) : AnnotationsProvider {
    private inline fun <T> withAnnotatedSymbol(crossinline action: context(KtAnalysisSession) (KtAnnotatedSymbol) -> T): T =
        annotatedSymbolPointer.withSymbol(ktModule, action)

    override fun classIds(): Collection<ClassId> = withAnnotatedSymbol { annotatedSymbol ->
        annotatedSymbol.annotationClassIds
    }

    override fun get(classId: ClassId): Collection<KtAnnotationApplication> = withAnnotatedSymbol { annotatedSymbol ->
        annotatedSymbol.annotationsByClassId(classId)
    }

    override fun contains(classId: ClassId): Boolean = withAnnotatedSymbol { annotatedSymbol ->
        annotatedSymbol.hasAnnotation(classId)
    }

    override fun isTheSameAs(other: Any?): Boolean = other === this ||
            other is SymbolAnnotationsProvider<*> &&
            other.ktModule == ktModule &&
            annotatedSymbolPointer.pointsToTheSameSymbolAs(other.annotatedSymbolPointer)

    override fun ownerClassId(): ClassId? = withAnnotatedSymbol { annotatedSymbol ->
        (annotatedSymbol as? KtClassLikeSymbol)?.classIdIfNonLocal
    }
}
