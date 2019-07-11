/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.symbols.impl.FirDelegateFieldSymbol
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.name.Name

interface FirDelegateFieldReference : FirResolvedCallableReference {

    override val name: Name
        get() = NAME

    override val coneSymbol: FirDelegateFieldSymbol<*>

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitDelegateFieldReference(this, data)

    companion object {
        val NAME = Name.identifier("\$delegate")
    }
}