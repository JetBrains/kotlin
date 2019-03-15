/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.types

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.types.impl.FirUserTypeRefImpl
import org.jetbrains.kotlin.load.java.structure.JavaType

class FirJavaTypeRef(
    session: FirSession,
    annotations: List<FirAnnotationCall>,
    val type: JavaType
) : FirUserTypeRefImpl(session, psi = null,isMarkedNullable = false) {
    init {
        this.annotations += annotations
    }
}