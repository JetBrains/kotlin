/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types.jvm

import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.types.impl.FirUserTypeRefImpl
import org.jetbrains.kotlin.load.java.structure.JavaType

class FirJavaTypeRef(
    annotations: List<FirAnnotationCall>,
    val type: JavaType
) : FirUserTypeRefImpl(source = null, isMarkedNullable = false) {
    init {
        this.annotations += annotations
    }
}