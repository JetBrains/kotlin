/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.enhancement

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.java.convertAnnotationsToFir
import org.jetbrains.kotlin.load.java.structure.JavaAnnotationOwner

/**
 * This class provides unified entry point for enhanced Java annotations.
 *
 * All FIR Java declarations delegates [org.jetbrains.kotlin.fir.FirAnnotationContainer.annotations] to [getAnnotations].
 *
 * TODO: the lazy annotations is a workaround for KT-55387, some non-lazy solution should probably be used instead
 *
 * @see FirLazyJavaAnnotationList
 * @see FirEmptyJavaAnnotationList
 */
fun interface FirJavaAnnotationList {
    fun getAnnotations(): List<FirAnnotation>
}

class FirLazyJavaAnnotationList(
    private val annotationOwner: JavaAnnotationOwner,
    private val ownerModuleData: FirModuleData,
    private val source: KtSourceElement?,
) : FirJavaAnnotationList {
    override fun getAnnotations(): List<FirAnnotation> = value

    private val value: List<FirAnnotation> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        annotationOwner.annotations.convertAnnotationsToFir(
            ownerModuleData.session,
            source?.fakeElement(KtFakeSourceElementKind.Enhancement),
            annotationOwner.isDeprecatedInJavaDoc,
        )
    }
}

object FirEmptyJavaAnnotationList : FirJavaAnnotationList {
    override fun getAnnotations(): List<FirAnnotation> = emptyList()
}
