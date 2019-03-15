/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirImport
import org.jetbrains.kotlin.fir.declarations.FirResolvedImport
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class FirResolvedImportImpl(
    val delegate: FirImport,
    override val resolvedFqName: ClassId
) : FirResolvedImport, FirImport {
    override val packageFqName: FqName
        get() = resolvedFqName.packageFqName

    override val relativeClassName: FqName
        get() = resolvedFqName.relativeClassName

    override val aliasName: Name?
        get() = delegate.aliasName

    override val importedFqName: FqName?
        get() = delegate.importedFqName

    override val isAllUnder: Boolean
        get() = delegate.isAllUnder

    override val psi: PsiElement?
        get() = delegate.psi

    override val session: FirSession
        get() = delegate.session
}