/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.compare

import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.resolve.transformers.FirAbstractTreeTransformer
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.compose

class FirPartialTransformer(
    private val visitAnnotation: Boolean = true
) : FirAbstractTreeTransformer() {
    override fun transformAnnotationCall(annotationCall: FirAnnotationCall, data: Nothing?): CompositeTransformResult<FirAnnotationCall> {
        return if (visitAnnotation) {
            (annotationCall.transformChildren(this, data) as FirAnnotationCall).compose()
        } else {
            CompositeTransformResult.empty()
        }
    }
}