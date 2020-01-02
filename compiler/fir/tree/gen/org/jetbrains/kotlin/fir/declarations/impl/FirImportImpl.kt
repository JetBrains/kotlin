/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.declarations.FirImport
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

class FirImportImpl(
    override val source: FirSourceElement?,
    override val importedFqName: FqName?,
    override val isAllUnder: Boolean,
    override val aliasName: Name?
) : FirImport() {
    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {}

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirImportImpl {
        return this
    }
}
