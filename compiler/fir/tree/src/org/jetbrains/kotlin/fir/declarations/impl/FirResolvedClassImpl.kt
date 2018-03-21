/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirBasedDescriptor
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirResolvedClass
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid

class FirResolvedClassImpl(val delegate: FirClass, override val descriptor: FirBasedDescriptor<FirResolvedClass>) :
    FirResolvedClass, FirClass by delegate {

    init {
        descriptor.bind(this)
    }

    override val modality: Modality
        get() = delegate.modality ?: if (classKind == ClassKind.INTERFACE) Modality.ABSTRACT else Modality.FINAL

    override fun accept(visitor: FirVisitorVoid) {
        super<FirResolvedClass>.accept(visitor)
    }

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R {
        return super<FirResolvedClass>.accept(visitor, data)
    }
}