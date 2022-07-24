/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.fir.references.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class FirExplicitSuperReference(
    override val source: KtSourceElement?,
    override val labelName: String?,
    override var superTypeRef: FirTypeRef,
) : FirSuperReference() {
    override val elementKind get() = FirElementKind.SuperReference

    override fun replaceSuperTypeRef(newSuperTypeRef: FirTypeRef) {
        superTypeRef = newSuperTypeRef
    }
}
