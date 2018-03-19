/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import org.jetbrains.kotlin.fir.FirBasedDescriptor
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirResolvedClass

class FirResolvedClassImpl(val delegate: FirClass, override val descriptor: FirBasedDescriptor<FirResolvedClass>) :
    FirResolvedClass, FirClass by delegate {

    init {
        descriptor.bind(this)
    }

}