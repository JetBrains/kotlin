/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.references

import org.jetbrains.kotlin.fir.FirAbstractElement
import org.jetbrains.kotlin.fir.FirThisReference
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol

class FirImplicitThisReference(override val boundSymbol: AbstractFirBasedSymbol<*>) : FirAbstractElement(null), FirThisReference {
    override val labelName: String?
        get() = null
}