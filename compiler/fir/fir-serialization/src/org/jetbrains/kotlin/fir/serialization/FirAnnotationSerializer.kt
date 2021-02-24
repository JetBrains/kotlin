/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.serialization

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.serialization.constant.ConstantValue
import org.jetbrains.kotlin.fir.serialization.constant.toConstantValue
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.metadata.ProtoBuf

class FirAnnotationSerializer(private val session: FirSession, internal val stringTable: FirElementAwareStringTable) {
    fun serializeAnnotation(annotation: FirAnnotationCall): ProtoBuf.Annotation = ProtoBuf.Annotation.newBuilder().apply {
        val annotationSymbol = annotation.typeRef.coneTypeSafe<ConeClassLikeType>()?.lookupTag?.toSymbol(session)
        val annotationClass = annotationSymbol?.fir ?: error("Annotation type is not a class: ${annotationSymbol?.fir}")

        id = stringTable.getFqNameIndex(annotationClass)

        for (argumentExpression in annotation.argumentList.arguments) {
            if (argumentExpression !is FirNamedArgumentExpression) continue
            val argument = ProtoBuf.Annotation.Argument.newBuilder()
            argument.nameId = stringTable.getStringIndex(argumentExpression.name.asString())
            argument.setValue(valueProto(argumentExpression.toConstantValue() ?: continue))
            addArgument(argument)
        }
    }.build()

    internal fun valueProto(constant: ConstantValue<*>): ProtoBuf.Annotation.Argument.Value.Builder =
        ProtoBuf.Annotation.Argument.Value.newBuilder().apply {
            constant.accept(
                FirAnnotationArgumentVisitor,
                FirAnnotationArgumentVisitorData(this@FirAnnotationSerializer, this)
            )
        }
}
