/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.serialization

import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.constant.AnnotationValue
import org.jetbrains.kotlin.constant.ConstantValue
import org.jetbrains.kotlin.constant.ErrorValue
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.serialization.constant.ConstValueProvider
import org.jetbrains.kotlin.fir.serialization.constant.toConstantValue
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

class FirAnnotationSerializer(
    private val session: FirSession,
    private val scopeSession: ScopeSession,
    internal val stringTable: FirElementAwareStringTable,
    private val constValueProvider: ConstValueProvider?
) {
    fun serializeAnnotation(annotation: FirAnnotation): ProtoBuf.Annotation {
        val annotationValue = annotation.toConstantValue<AnnotationValue>(session, scopeSession, constValueProvider)
            ?: error("Cannot serialize annotation ${annotation.render()}")
        return serializeAnnotation(annotationValue)
    }

    fun serializeAnnotation(annotation: AnnotationValue): ProtoBuf.Annotation {
        return serializeAnnotation(annotation.value.classId, annotation.value.argumentsMapping)
    }

    private fun serializeAnnotation(classId: ClassId, argumentsMapping: Map<Name, ConstantValue<*>>): ProtoBuf.Annotation {
        return ProtoBuf.Annotation.newBuilder().apply {
            id = stringTable.getQualifiedClassNameIndex(classId)

            fun addArgument(argumentExpression: ConstantValue<*>, parameterName: Name) {
                val argument = ProtoBuf.Annotation.Argument.newBuilder()
                argument.nameId = stringTable.getStringIndex(parameterName.asString())
                argument.setValue(valueProto(argumentExpression))
                addArgument(argument)
            }

            for ((name, argument) in argumentsMapping) {
                if (argument !is ErrorValue) {
                    addArgument(argument, name)
                    continue
                }

                if (!session.languageVersionSettings.getFlag(AnalysisFlags.metadataCompilation)) {
                    error(
                        (argument as? ErrorValue.ErrorValueWithMessage)?.message
                            ?: "Error value after conversion of expression of $name argument"
                    )
                }
            }
        }.build()
    }

    internal fun valueProto(constant: ConstantValue<*>): ProtoBuf.Annotation.Argument.Value.Builder =
        ProtoBuf.Annotation.Argument.Value.newBuilder().apply {
            constant.accept(
                FirAnnotationArgumentVisitor,
                FirAnnotationArgumentVisitorData(this@FirAnnotationSerializer, this)
            )
        }
}
