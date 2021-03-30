/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types.jvm

import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.types.FirQualifierPart
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.FirUserTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.fir.visitors.transformInplace
import org.jetbrains.kotlin.load.java.structure.JavaArrayType
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType
import org.jetbrains.kotlin.load.java.structure.JavaPrimitiveType
import org.jetbrains.kotlin.load.java.structure.JavaType

class FirJavaTypeRef(
    val type: JavaType,
    annotationBuilder: () -> List<FirAnnotationCall>,
    override val qualifier: MutableList<FirQualifierPart>
) : FirUserTypeRef(), FirAnnotationContainer {
    override val customRenderer: Boolean
        get() = true

    override val isMarkedNullable: Boolean
        get() = false

    override val source: FirSourceElement?
        get() = null

    override val annotations: List<FirAnnotationCall> by lazy { annotationBuilder() }

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        for (part in qualifier) {
            part.typeArgumentList.typeArguments.forEach { it.accept(visitor, data) }
        }
        annotations.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirUserTypeRef {
        for (part in qualifier) {
            (part.typeArgumentList.typeArguments as MutableList<FirTypeProjection>).transformInplace(transformer, data)
        }
        return this
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirUserTypeRef {
        return this
    }

    override fun toString(): String {
        return type.render()
    }
}

@FirBuilderDsl
class FirJavaTypeRefBuilder {
    lateinit var annotationBuilder: () -> List<FirAnnotationCall>
    lateinit var type: JavaType
    val qualifier: MutableList<FirQualifierPart> = mutableListOf()

    fun build(): FirJavaTypeRef {
        return FirJavaTypeRef(type, annotationBuilder, qualifier)
    }
}

inline fun buildJavaTypeRef(init: FirJavaTypeRefBuilder.() -> Unit): FirJavaTypeRef {
    return FirJavaTypeRefBuilder().apply(init).build()
}

private fun JavaType?.render(): String {
    return when (this) {
        is JavaArrayType -> "${componentType.render()}[]"
        is JavaClassifierType -> if (typeArguments.isEmpty()) {
            classifierQualifiedName
        } else {
            classifierQualifiedName + typeArguments.joinToString(separator = ", ", prefix = "<", postfix = ">") { it.render() }
        }
        is JavaPrimitiveType -> type?.typeName?.identifier ?: "void"
        else -> toString()
    }
}
