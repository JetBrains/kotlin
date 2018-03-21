/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import org.jetbrains.kotlin.fir.FirBasedDescriptor
import org.jetbrains.kotlin.fir.declarations.FirResolvedTypeAlias
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.visitors.FirVisitor

class FirResolvedTypeAliasImpl(val delegate: FirTypeAlias, override val descriptor: FirBasedDescriptor<FirResolvedTypeAlias>) :
    FirResolvedTypeAlias, FirTypeAlias by delegate {

    init {
        descriptor.bind(this)
    }

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R {
        return super<FirResolvedTypeAlias>.accept(visitor, data)
    }
}