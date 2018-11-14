/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import org.jetbrains.kotlin.fir.declarations.FirImport
import org.jetbrains.kotlin.fir.declarations.FirResolvedImport
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

class FirResolvedImportImpl(
    val delegate: FirImport,
    override val resolvedFqName: ClassId
) : FirResolvedImport, FirImport by delegate {
    override val packageFqName: FqName
        get() = resolvedFqName.packageFqName

    override val relativeClassName: FqName
        get() = resolvedFqName.relativeClassName

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitResolvedImport(this, data)
}