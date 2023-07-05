/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.fir.references.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.references.FirDelegateFieldReference
import org.jetbrains.kotlin.fir.references.impl.FirDelegateFieldReferenceImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirDelegateFieldSymbol
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.name.Name

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirDelegateFieldReferenceBuilder {
    var source: KtSourceElement? = null
    lateinit var resolvedSymbol: FirDelegateFieldSymbol

    fun build(): FirDelegateFieldReference {
        return FirDelegateFieldReferenceImpl(
            source,
            resolvedSymbol,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildDelegateFieldReference(init: FirDelegateFieldReferenceBuilder.() -> Unit): FirDelegateFieldReference {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirDelegateFieldReferenceBuilder().apply(init).build()
}
