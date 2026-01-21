/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.symbols.KaBackingFieldSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaAnnotatedSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.light.classes.symbol.withSymbol
import org.jetbrains.kotlin.name.ClassId

internal class SymbolAnnotationsProvider<T : KaAnnotatedSymbol>(
    private val ktModule: KaModule,
    private val annotatedSymbolPointer: KaSymbolPointer<T>,
) : AnnotationsProvider {
    private inline fun <T> withAnnotatedSymbol(crossinline action: KaSession.(KaAnnotatedSymbol) -> T): T =
        annotatedSymbolPointer.withSymbol(ktModule, action)

    @field:Volatile
    var lastAccessedAnnotationIds: Collection<ClassId>? = null

    @field:Volatile
    var lastAnnotatedSymbolClassAsString: String? = null

    @field:Volatile
    var lastSymbolAnnotationClass: String? = null

    @field:Volatile
    var lastExtraSymbolInfo: List<String>? = null

    @field:Volatile
    var owningPropertyInfoForBackingField: List<String>? = null

    override fun annotationInfos(): List<AnnotationApplication> = withAnnotatedSymbol { annotatedSymbol ->
        val indices = mutableMapOf<ClassId?, Int>()
        lastAccessedAnnotationIds = annotatedSymbol.annotations.classIds
        lastAnnotatedSymbolClassAsString = annotatedSymbol::class.qualifiedName
        lastSymbolAnnotationClass = annotatedSymbol.annotations::class.qualifiedName
        lastExtraSymbolInfo = annotatedSymbol.getExtraInfo()

        if (annotatedSymbol is KaBackingFieldSymbol) { // expected from the debug info
            owningPropertyInfoForBackingField = buildList {
                add("Backing field owning property class: ${annotatedSymbol.owningProperty::class.qualifiedName}")
                add("Backing field owning property annotations class: ${annotatedSymbol.owningProperty.annotations::class.qualifiedName}")
                add(
                    "Backing field owning property annotations class IDs: ${
                        annotatedSymbol.owningProperty.annotations.classIds.joinToString(
                            "; "
                        ) { it.asString() }
                    }"
                )
            }
        }

        annotatedSymbol.annotations.map { annotation ->
            // to preserve the initial annotations order
            val index = indices.merge(annotation.classId, 0) { old, _ -> old + 1 }!!
            annotation.toDumbLightClassAnnotationApplication(index)
        }
    }

    override fun get(classId: ClassId): List<AnnotationApplication> = withAnnotatedSymbol { annotatedSymbol ->
        annotatedSymbol.annotations[classId].mapIndexed { index, annotation -> annotation.toLightClassAnnotationApplication(index) }
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
