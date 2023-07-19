/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.FirDiagnosticHolder
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirErrorImport : FirImport(), FirDiagnosticHolder {
    abstract override val source: KtSourceElement?
    abstract override val importedFqName: FqName?
    abstract override val isAllUnder: Boolean
    abstract override val aliasName: Name?
    abstract override val aliasSource: KtSourceElement?
    abstract override val diagnostic: ConeDiagnostic
    abstract val delegate: FirImport

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitErrorImport(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : FirElement, D> transform(transformer: FirTransformer<D>, data: D): E =
        transformer.transformErrorImport(this, data) as E
}
