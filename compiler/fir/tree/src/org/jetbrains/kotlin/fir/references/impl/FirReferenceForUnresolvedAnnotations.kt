/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.references.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.visitors.FirElementKind
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor

object FirReferenceForUnresolvedAnnotations : FirReference() {
    override val source: KtSourceElement?
        get() = null

    override val elementKind: FirElementKind
        get() = FirElementKind.Reference
}