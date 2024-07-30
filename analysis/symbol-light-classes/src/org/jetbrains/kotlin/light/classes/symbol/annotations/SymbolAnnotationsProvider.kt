/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaAnnotatedSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.light.classes.symbol.withSymbol
import org.jetbrains.kotlin.name.ClassId

internal class SymbolAnnotationsProvider<T : KaAnnotatedSymbol>(
    private val ktModule: KaModule,
    private val annotatedSymbolPointer: KaSymbolPointer<T>,
) : AnnotationsProvider {
    @Suppress("CONTEXT_RECEIVERS_DEPRECATED")
    private inline fun <T> withAnnotatedSymbol(crossinline action: context(KaSession) (KaAnnotatedSymbol) -> T): T =
        annotatedSymbolPointer.withSymbol(ktModule, action)

    override fun annotationInfos(): List<AnnotationApplication> = withAnnotatedSymbol { annotatedSymbol ->
        annotatedSymbol.annotations.map { it.toDumbLightClassAnnotationApplication() }
    }

    override fun get(classId: ClassId): Collection<AnnotationApplication> = withAnnotatedSymbol { annotatedSymbol ->
        annotatedSymbol.annotations[classId].map { it.toLightClassAnnotationApplication() }
    }

    override fun contains(classId: ClassId): Boolean = withAnnotatedSymbol { annotatedSymbol ->
        classId in annotatedSymbol.annotations
    }

    override fun equals(other: Any?): Boolean = other === this ||
            other is SymbolAnnotationsProvider<*> &&
            other.ktModule == ktModule &&
            annotatedSymbolPointer.pointsToTheSameSymbolAs(other.annotatedSymbolPointer)

    override fun hashCode(): Int = annotatedSymbolPointer.hashCode()

    override fun ownerClassId(): ClassId? = withAnnotatedSymbol { annotatedSymbol ->
        (annotatedSymbol as? KaClassLikeSymbol)?.classId
    }
}
