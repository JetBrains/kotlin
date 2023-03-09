/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.serialization.constant

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.FirConstExpression
import org.jetbrains.kotlin.fir.serialization.FirAnnotationSerializer
import org.jetbrains.kotlin.metadata.ProtoBuf

abstract class ConstValueProvider {
    abstract val session: FirSession

    abstract fun getConstantValueForProperty(firProperty: FirProperty): FirConstExpression<*>?

    fun buildValueProtoBufIfPropertyIsConst(
        firProperty: FirProperty, annotationSerializer: FirAnnotationSerializer
    ): ProtoBuf.Annotation.Argument.Value? {
        return getConstantValueForProperty(firProperty).toProtoBuf(annotationSerializer)
    }

    private fun FirConstExpression<*>?.toProtoBuf(
        annotationSerializer: FirAnnotationSerializer
    ): ProtoBuf.Annotation.Argument.Value? {
        val constantValue = this?.toConstantValue(session) ?: return null
        return annotationSerializer.valueProto(constantValue).build()
    }
}

