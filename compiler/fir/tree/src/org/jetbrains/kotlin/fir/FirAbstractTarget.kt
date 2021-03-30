/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

abstract class FirAbstractTarget<E : FirTargetElement>(
    override val labelName: String?
) : FirTarget<E> {
    protected abstract var _labeledElement: E

    final override val labeledElement: E
        get() = _labeledElement

    override fun bind(element: E) {
        _labeledElement = element
    }
}
