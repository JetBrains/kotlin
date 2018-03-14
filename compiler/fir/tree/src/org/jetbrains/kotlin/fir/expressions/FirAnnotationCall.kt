/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.types.FirType
import org.jetbrains.kotlin.fir.visitors.FirVisitor

interface FirAnnotationCall : FirCall {
    val annotationType: FirType

    // May be should be not-null (with correct default target)
    val useSiteTarget: AnnotationUseSiteTarget?

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitAnnotationCall(this, data)

    override fun <D> acceptChildren(visitor: FirVisitor<Unit, D>, data: D) {
        annotationType.accept(visitor, data)
        super.acceptChildren(visitor, data)
    }
}