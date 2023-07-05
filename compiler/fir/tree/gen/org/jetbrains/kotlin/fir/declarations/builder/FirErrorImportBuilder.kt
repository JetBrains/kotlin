/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.fir.declarations.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.declarations.FirErrorImport
import org.jetbrains.kotlin.fir.declarations.FirImport
import org.jetbrains.kotlin.fir.declarations.impl.FirErrorImportImpl
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirErrorImportBuilder {
    var aliasSource: KtSourceElement? = null
    lateinit var diagnostic: ConeDiagnostic
    lateinit var delegate: FirImport

    fun build(): FirErrorImport {
        return FirErrorImportImpl(
            aliasSource,
            diagnostic,
            delegate,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildErrorImport(init: FirErrorImportBuilder.() -> Unit): FirErrorImport {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirErrorImportBuilder().apply(init).build()
}
