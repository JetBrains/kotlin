/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.serialization.constant

import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.FirReceiverParameter
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.serialization.FirAnnotationSerializer
import org.jetbrains.kotlin.metadata.ProtoBuf

abstract class ConstValueProvider {
    abstract val session: FirSession

    abstract fun getConstantValueForProperty(firProperty: FirProperty): FirExpression?

    abstract fun getNewFirAnnotationWithConstantValues(
        firAnnotationContainer: FirAnnotationContainer,
        firAnnotation: FirAnnotation,
    ): FirAnnotation

    abstract fun getNewFirAnnotationWithConstantValues(
        firExtensionReceiverContainer: FirAnnotationContainer,
        firAnnotation: FirAnnotation,
        receiverParameter: FirReceiverParameter,
    ): FirAnnotation

    fun FirExpression?.toProtoBuf(annotationSerializer: FirAnnotationSerializer): ProtoBuf.Annotation.Argument.Value? {
        val constantValue = this?.toConstantValue(session) ?: return null
        return annotationSerializer.valueProto(constantValue).build()
    }
}

fun ConstValueProvider.buildValueProtoBufIfPropertyHasConst(
    firProperty: FirProperty, annotationSerializer: FirAnnotationSerializer
): ProtoBuf.Annotation.Argument.Value? {
    return getConstantValueForProperty(firProperty).toProtoBuf(annotationSerializer)
}
