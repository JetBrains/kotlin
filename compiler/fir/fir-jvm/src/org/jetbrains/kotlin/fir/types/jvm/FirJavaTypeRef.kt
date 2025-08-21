/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types.jvm

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.load.java.structure.JavaArrayType
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType
import org.jetbrains.kotlin.load.java.structure.JavaPrimitiveType
import org.jetbrains.kotlin.load.java.structure.JavaType

class FirJavaTypeRef(
    val type: JavaType,
    override val source: KtSourceElement?,
    annotationBuilder: () -> List<FirAnnotation>
) : FirTypeRef() {
    override val customRenderer: Boolean
        get() = true

    override val annotations: List<FirAnnotation> by lazy { annotationBuilder() }

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirTypeRef {
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        throw AssertionError("Mutating annotations for FirJava* is not supported")
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirTypeRef {
        return this
    }

    override fun toString(): String {
        return type.render()
    }
}

@FirBuilderDsl
class FirJavaTypeRefBuilder {
    lateinit var annotationBuilder: () -> List<FirAnnotation>
    lateinit var type: JavaType
    var source: KtSourceElement? = null

    fun build(): FirJavaTypeRef {
        return FirJavaTypeRef(type, source, annotationBuilder)
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
