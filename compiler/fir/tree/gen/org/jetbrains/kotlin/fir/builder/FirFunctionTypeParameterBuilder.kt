/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.fir.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirFunctionTypeParameter
import org.jetbrains.kotlin.fir.impl.FirFunctionTypeParameterImpl
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name

@FirBuilderDsl
class FirFunctionTypeParameterBuilder {
    var source: KtSourceElement? = null
    var name: Name? = null
    lateinit var returnTypeRef: FirTypeRef

    fun build(): FirFunctionTypeParameter {
        return FirFunctionTypeParameterImpl(
            source,
            name,
            returnTypeRef,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildFunctionTypeParameter(init: FirFunctionTypeParameterBuilder.() -> Unit): FirFunctionTypeParameter {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirFunctionTypeParameterBuilder().apply(init).build()
}
