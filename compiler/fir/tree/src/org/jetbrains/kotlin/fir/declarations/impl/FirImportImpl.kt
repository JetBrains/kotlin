/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirImport
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class FirImportImpl(
    override val session: FirSession,
    override val psi: PsiElement?,
    override val importedFqName: FqName?,
    override val isAllUnder: Boolean,
    override val aliasName: Name?
) : FirImport