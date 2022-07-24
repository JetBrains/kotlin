/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.fir.declarations.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.declarations.FirErrorImport
import org.jetbrains.kotlin.fir.declarations.FirImport
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class FirErrorImportImpl(
    override val aliasSource: KtSourceElement?,
    override val diagnostic: ConeDiagnostic,
    override var delegate: FirImport,
) : FirErrorImport() {
    override val source: KtSourceElement? get() = delegate.source
    override val importedFqName: FqName? get() = delegate.importedFqName
    override val isAllUnder: Boolean get() = delegate.isAllUnder
    override val aliasName: Name? get() = delegate.aliasName

    override val elementKind get() = FirElementKind.ErrorImport

    override fun replaceDelegate(newDelegate: FirImport) {
        delegate = newDelegate
    }
}
