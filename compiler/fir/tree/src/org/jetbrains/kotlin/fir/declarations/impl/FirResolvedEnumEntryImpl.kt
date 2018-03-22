/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import org.jetbrains.kotlin.fir.FirBasedDescriptor
import org.jetbrains.kotlin.fir.declarations.FirEnumEntry
import org.jetbrains.kotlin.fir.declarations.FirResolvedEnumEntry
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid

class FirResolvedEnumEntryImpl(
    val delegate: FirEnumEntry,
    override val descriptor: FirBasedDescriptor<FirResolvedEnumEntry>
) : FirResolvedEnumEntry, FirEnumEntry by delegate {

    init {
        descriptor.bind(this)
    }

    override fun accept(visitor: FirVisitorVoid) {
        super<FirResolvedEnumEntry>.accept(visitor)
    }

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R {
        return super<FirResolvedEnumEntry>.accept(visitor, data)
    }
}