/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.fir.declarations.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.declarations.FirImport
import org.jetbrains.kotlin.fir.declarations.FirResolvedImport
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedImportImpl
import org.jetbrains.kotlin.name.FqName

@FirBuilderDsl
class FirResolvedImportBuilder {
    lateinit var delegate: FirImport
    lateinit var packageFqName: FqName
    var relativeParentClassName: FqName? = null

    fun build(): FirResolvedImport {
        return FirResolvedImportImpl(
            delegate,
            packageFqName,
            relativeParentClassName,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildResolvedImport(init: FirResolvedImportBuilder.() -> Unit): FirResolvedImport {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirResolvedImportBuilder().apply(init).build()
}
