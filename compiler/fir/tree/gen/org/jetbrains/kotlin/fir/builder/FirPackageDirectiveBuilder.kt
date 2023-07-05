/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.fir.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirPackageDirective
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.impl.FirPackageDirectiveImpl
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.name.FqName

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirPackageDirectiveBuilder {
    var source: KtSourceElement? = null
    lateinit var packageFqName: FqName

    fun build(): FirPackageDirective {
        return FirPackageDirectiveImpl(
            source,
            packageFqName,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildPackageDirective(init: FirPackageDirectiveBuilder.() -> Unit): FirPackageDirective {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirPackageDirectiveBuilder().apply(init).build()
}
