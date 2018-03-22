/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirBasedDescriptor
import org.jetbrains.kotlin.fir.declarations.FirResolvedTypeAlias
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid

class FirResolvedTypeAliasImpl(val delegate: FirTypeAlias, override val descriptor: FirBasedDescriptor<FirResolvedTypeAlias>) :
    FirResolvedTypeAlias, FirTypeAlias by delegate {

    override val modality: Modality
        get() = delegate.modality ?: Modality.FINAL

    init {
        symbol.bind(this)
        descriptor.bind(this)
    }

    override fun accept(visitor: FirVisitorVoid) {
        return super<FirResolvedTypeAlias>.accept(visitor)
    }

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R {
        return super<FirResolvedTypeAlias>.accept(visitor, data)
    }
}