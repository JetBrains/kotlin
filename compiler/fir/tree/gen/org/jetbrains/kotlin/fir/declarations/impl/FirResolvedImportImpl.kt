/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.declarations.FirImport
import org.jetbrains.kotlin.fir.declarations.FirResolvedImport
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

class FirResolvedImportImpl(
    override var delegate: FirImport,
    override val packageFqName: FqName,
    override val relativeClassName: FqName?
) : FirResolvedImport() {
    override val source: FirSourceElement? get() = delegate.source
    override val importedFqName: FqName? get() = delegate.importedFqName
    override val isAllUnder: Boolean get() = delegate.isAllUnder
    override val aliasName: Name? get() = delegate.aliasName
    override val resolvedClassId: ClassId? get() = relativeClassName?.let { ClassId(packageFqName, it, false) }
    override val importedName: Name? get() = importedFqName?.shortName()

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirResolvedImportImpl {
        return this
    }
}
