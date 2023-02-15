/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.psi.KtTypeReference

class StubBasedFirBuiltinAnnotationDeserializer(
    session: FirSession
) : StubBasedAbstractAnnotationDeserializer(session) {

    override fun loadTypeAnnotations(typeReference: KtTypeReference): List<FirAnnotation> {
        val ktAnnotations = typeReference.annotationEntries
        if (ktAnnotations.isEmpty()) return emptyList()
        return ktAnnotations.map { deserializeAnnotation(it) }
    }
}
