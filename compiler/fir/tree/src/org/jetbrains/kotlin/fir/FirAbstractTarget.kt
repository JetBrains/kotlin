/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

abstract class FirAbstractTarget<E : FirTargetElement>(
    override val labelName: String?
) : FirTarget<E> {
    override lateinit var labeledElement: E

    override fun bind(element: E) {
        labeledElement = element
    }
}