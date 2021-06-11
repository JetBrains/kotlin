/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.references.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.references.impl.FirExplicitThisReference
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirExplicitThisReferenceBuilder {
    var source: FirSourceElement? = null
    var labelName: String? = null

    fun build(): FirThisReference {
        return FirExplicitThisReference(
            source,
            labelName,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildExplicitThisReference(init: FirExplicitThisReferenceBuilder.() -> Unit = {}): FirThisReference {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirExplicitThisReferenceBuilder().apply(init).build()
}
