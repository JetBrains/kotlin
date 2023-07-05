/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.fir.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirFunctionTypeParameter
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.impl.FirFunctionTypeParameterImpl
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.name.Name

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

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
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirFunctionTypeParameterBuilder().apply(init).build()
}
