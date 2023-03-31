/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.serialization

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.serialization.constant.ConstantValue
import org.jetbrains.kotlin.fir.serialization.constant.toConstantValue
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.name.Name

class FirAnnotationSerializer(private val session: FirSession, internal val stringTable: FirElementAwareStringTable) {
    fun serializeAnnotation(annotation: FirAnnotation): ProtoBuf.Annotation = ProtoBuf.Annotation.newBuilder().apply {
        val lookupTag = annotation.typeRef.coneTypeSafe<ConeClassLikeType>()?.lookupTag
            ?: error { "Annotation without proper lookup tag: ${annotation.annotationTypeRef.coneType}" }

        id = lookupTag.toSymbol(session)?.let { stringTable.getFqNameIndex(it.fir) }
            ?: stringTable.getQualifiedClassNameIndex(lookupTag.classId)

        fun addArgument(argumentExpression: FirExpression, parameterName: Name) {
            val argument = ProtoBuf.Annotation.Argument.newBuilder()
            argument.nameId = stringTable.getStringIndex(parameterName.asString())
            val constantValue = argumentExpression.toConstantValue(session)
                ?: error("Cannot convert expression ${argumentExpression.render()} to constant")
            argument.setValue(valueProto(constantValue))
            addArgument(argument)
        }

        for ((name, argument) in annotation.argumentMapping.mapping) {
            addArgument(argument, name)
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
