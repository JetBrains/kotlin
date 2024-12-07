/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types.builder

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.types.FirQualifierPart
import org.jetbrains.kotlin.fir.types.FirUserTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirUserTypeRefImpl


@FirBuilderDsl
open class FirUserTypeRefBuilder : FirAnnotationContainerBuilder {
    override var source: KtSourceElement? = null
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    open var isMarkedNullable: Boolean by kotlin.properties.Delegates.notNull()
    val qualifier: MutableList<FirQualifierPart> = mutableListOf()

    override fun build(): FirUserTypeRef {
        return FirUserTypeRefImpl(source, isMarkedNullable, qualifier, annotations.toMutableOrEmpty())
    }
}

inline fun buildUserTypeRef(init: FirUserTypeRefBuilder.() -> Unit): FirUserTypeRef {
    return FirUserTypeRefBuilder().apply(init).build()
}
