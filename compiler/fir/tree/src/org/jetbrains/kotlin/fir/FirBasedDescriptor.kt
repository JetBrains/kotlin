/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.descriptors.ConeDescriptor

interface FirBasedDescriptor<E> : ConeDescriptor where E : FirElement, E : FirDescriptorOwner<E> {
    val fir: E
    fun bind(e: E)
}