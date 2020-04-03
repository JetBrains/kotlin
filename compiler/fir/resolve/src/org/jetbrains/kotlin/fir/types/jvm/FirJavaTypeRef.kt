/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types.jvm

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.types.FirQualifierPart
import org.jetbrains.kotlin.fir.types.FirUserTypeRef
import org.jetbrains.kotlin.fir.types.builder.FirUserTypeRefBuilder
import org.jetbrains.kotlin.fir.types.impl.FirUserTypeRefImpl
import org.jetbrains.kotlin.load.java.structure.JavaType

class FirJavaTypeRef(
    val type: JavaType,
    annotations: MutableList<FirAnnotationCall>,
    qualifier: MutableList<FirQualifierPart>
) : FirUserTypeRefImpl(
    source = null,
    isMarkedNullable = false,
    qualifier,
    annotations
)

@FirBuilderDsl
class FirJavaTypeRefBuilder : FirUserTypeRefBuilder() {
    override val annotations: MutableList<FirAnnotationCall> = mutableListOf()
    lateinit var type: JavaType

    override fun build(): FirJavaTypeRef {
        return FirJavaTypeRef(type, annotations, qualifier)
    }

    @Deprecated("Modification of 'source' has no impact for FirJavaTypeRefBuilder", level = DeprecationLevel.HIDDEN)
    override var source: FirSourceElement? = null

    @Deprecated("Modification of 'isMarkedNullable' has no impact for FirJavaTypeRefBuilder", level = DeprecationLevel.HIDDEN)
    override var isMarkedNullable: Boolean = false
}

inline fun buildJavaTypeRef(init: FirJavaTypeRefBuilder.() -> Unit): FirJavaTypeRef {
    return FirJavaTypeRefBuilder().apply(init).build()
}