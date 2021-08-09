/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.enhancement

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.load.java.*

class FirJavaEnhancementContext private constructor(
    val session: FirSession,
    delegateForDefaultTypeQualifiers: Lazy<JavaTypeQualifiersByElementType?>
) {
    constructor(session: FirSession, typeQualifiersComputation: () -> JavaTypeQualifiersByElementType?) :
            this(session, lazy(LazyThreadSafetyMode.NONE, typeQualifiersComputation))

    val defaultTypeQualifiers: JavaTypeQualifiersByElementType? by delegateForDefaultTypeQualifiers
}

fun FirJavaEnhancementContext.copyWithNewDefaultTypeQualifiers(
    typeQualifierResolver: FirAnnotationTypeQualifierResolver,
    additionalAnnotations: List<FirAnnotationCall>
): FirJavaEnhancementContext =
    when {
        additionalAnnotations.isEmpty() -> this
        else -> FirJavaEnhancementContext(session) {
            typeQualifierResolver.extractAndMergeDefaultQualifiers(defaultTypeQualifiers, additionalAnnotations)
        }
    }
