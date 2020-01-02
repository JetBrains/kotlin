/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.references

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.diagnostics.FirDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.FirDiagnosticHolder
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirErrorNamedReference : FirNamedReference(), FirDiagnosticHolder {
    abstract override val source: FirSourceElement?
    abstract override val name: Name
    abstract override val candidateSymbol: AbstractFirBasedSymbol<*>?
    abstract override val diagnostic: FirDiagnostic

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitErrorNamedReference(this, data)
}
