/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.FirTarget
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirLoop
import org.jetbrains.kotlin.fir.expressions.FirLoopJump
import org.jetbrains.kotlin.fir.impl.FirAbstractAnnotatedElement
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImpl
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface FirAbstractLoopJump : FirLoopJump, FirAbstractAnnotatedElement {
    override val source: FirSourceElement?
    override var typeRef: FirTypeRef
    override val annotations: MutableList<FirAnnotationCall>
    override var target: FirTarget<FirLoop>
    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirAbstractLoopJump

    override fun replaceTypeRef(newTypeRef: FirTypeRef)
}
