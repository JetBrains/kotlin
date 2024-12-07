/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.enhancement

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.java.DeprecatedInJavaDocAnnotation
import org.jetbrains.kotlin.fir.java.convertAnnotationsToFir
import org.jetbrains.kotlin.fir.java.toSourceElement
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.load.java.structure.JavaAnnotationOwner
import java.util.*
import kotlin.collections.AbstractList

/**
 * This class provides unified entry point for enhanced Java annotations.
 *
 * All FIR Java declarations delegates [org.jetbrains.kotlin.fir.FirAnnotationContainer.annotations] to this class.
 *
 * TODO: the lazy annotations is a workaround for KT-55387, some non-lazy solution should probably be used instead
 *
 * @see FirLazyJavaAnnotationList
 * @see FirDelegatedJavaAnnotationList
 * @see FirEmptyJavaAnnotationList
 */
interface FirJavaAnnotationList : List<FirAnnotation>

class FirDelegatedJavaAnnotationList(annotationsOwner: FirAnnotationContainer) :
    FirJavaAnnotationList,
    List<FirAnnotation> by annotationsOwner.annotations

class FirLazyJavaAnnotationList(
    private val annotationOwner: JavaAnnotationOwner,
    private val ownerModuleData: FirModuleData,
) : FirJavaAnnotationList {
    private val javaAnnotations: Collection<JavaAnnotation> get() = annotationOwner.annotations

    private val firAnnotations: List<FirAnnotation> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val fakeSource = annotationOwner.toSourceElement(KtFakeSourceElementKind.Enhancement)
        javaAnnotations.convertAnnotationsToFir(
            ownerModuleData.session,
            fakeSource,
            annotationOwner.isDeprecatedInJavaDoc,
        )
    }

    /**
     * [DeprecatedInJavaDocAnnotation] is generated in the case [JavaAnnotationOwner.isDeprecatedInJavaDoc]
     */
    override fun isEmpty(): Boolean = javaAnnotations.isEmpty() && !annotationOwner.isDeprecatedInJavaDoc

    override val size: Int get() = firAnnotations.size
    override fun contains(element: FirAnnotation): Boolean = element in firAnnotations
    override fun iterator(): Iterator<FirAnnotation> = firAnnotations.iterator()
    override fun containsAll(elements: Collection<FirAnnotation>): Boolean = firAnnotations.containsAll(elements)
    override fun get(index: Int): FirAnnotation = firAnnotations[index]
    override fun indexOf(element: FirAnnotation): Int = firAnnotations.indexOf(element)
    override fun lastIndexOf(element: FirAnnotation): Int = firAnnotations.lastIndexOf(element)
    override fun listIterator(): ListIterator<FirAnnotation> = firAnnotations.listIterator()
    override fun listIterator(index: Int): ListIterator<FirAnnotation> = firAnnotations.listIterator(index)
    override fun subList(fromIndex: Int, toIndex: Int): List<FirAnnotation> = firAnnotations.subList(fromIndex, toIndex)
}

object FirEmptyJavaAnnotationList : FirJavaAnnotationList, AbstractList<FirAnnotation>() {
    override val size: Int get() = 0
    override fun isEmpty(): Boolean = true
    override fun contains(element: FirAnnotation): Boolean = false
    override fun iterator(): Iterator<FirAnnotation> = Collections.emptyIterator()
    override fun containsAll(elements: Collection<FirAnnotation>): Boolean = elements.isEmpty()
    override fun get(index: Int): FirAnnotation = throw IndexOutOfBoundsException("Index $index out of bounds")
    override fun listIterator(): ListIterator<FirAnnotation> = Collections.emptyListIterator()
}
