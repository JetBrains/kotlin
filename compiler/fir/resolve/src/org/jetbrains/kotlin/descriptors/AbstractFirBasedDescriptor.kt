/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors

import org.jetbrains.kotlin.fir.FirBasedDescriptor
import org.jetbrains.kotlin.fir.FirDescriptorOwner
import org.jetbrains.kotlin.fir.FirElement

abstract class AbstractFirBasedDescriptor<E> : FirBasedDescriptor<E> where E : FirElement, E : FirDescriptorOwner<E> {

    override lateinit var fir: E

    override fun bind(e: E) {
        fir = e
    }
}