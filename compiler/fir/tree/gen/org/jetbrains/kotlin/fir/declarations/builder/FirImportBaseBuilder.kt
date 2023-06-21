/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.fir.declarations.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.declarations.FirImportBase
import org.jetbrains.kotlin.fir.declarations.impl.FirImportBaseImpl
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirImportBaseBuilder {
    var source: KtSourceElement? = null
    var importedFqName: FqName? = null
    var isAllUnder: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    var aliasName: Name? = null
    var aliasSource: KtSourceElement? = null

    fun build(): FirImportBase {
        return FirImportBaseImpl(
            source,
            importedFqName,
            isAllUnder,
            aliasName,
            aliasSource,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildImportBase(init: FirImportBaseBuilder.() -> Unit): FirImportBase {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirImportBaseBuilder().apply(init).build()
}
