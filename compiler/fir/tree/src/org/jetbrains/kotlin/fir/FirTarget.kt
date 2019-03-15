/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

// Reference to some label
interface FirTarget<E : FirTargetElement> {
    val labelName: String?

    val labeledElement: E

    fun bind(element: E)
}