/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.fir.declarations.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.declarations.FirImport
import org.jetbrains.kotlin.fir.declarations.impl.FirImportImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

@FirBuilderDsl
class FirImportBuilder {
    var source: KtSourceElement? = null
    var importedFqName: FqName? = null
    var isAllUnder: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    var aliasName: Name? = null
    var aliasSource: KtSourceElement? = null

    fun build(): FirImport {
        return FirImportImpl(
            source,
            importedFqName,
            isAllUnder,
            aliasName,
            aliasSource,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildImport(init: FirImportBuilder.() -> Unit): FirImport {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirImportBuilder().apply(init).build()
}
