/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import org.jetbrains.kotlin.fir.FirBasedDescriptor
import org.jetbrains.kotlin.fir.declarations.FirResolvedTypeParameter
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid

class FirResolvedTypeParameterImpl(val delegate: FirTypeParameter, override val descriptor: FirBasedDescriptor<FirResolvedTypeParameter>) :
    FirResolvedTypeParameter, FirTypeParameter by delegate {

    init {
        symbol.bind(this)
        descriptor.bind(this)
    }

    override fun accept(visitor: FirVisitorVoid) {
        return super<FirResolvedTypeParameter>.accept(visitor)
    }

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R {
        return super<FirResolvedTypeParameter>.accept(visitor, data)
    }
}