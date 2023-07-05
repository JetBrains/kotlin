/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.fir.declarations.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.declarations.FirContextReceiver
import org.jetbrains.kotlin.fir.declarations.impl.FirContextReceiverImpl
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.name.Name

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirContextReceiverBuilder {
    var source: KtSourceElement? = null
    lateinit var typeRef: FirTypeRef
    var customLabelName: Name? = null
    var labelNameFromTypeRef: Name? = null

    fun build(): FirContextReceiver {
        return FirContextReceiverImpl(
            source,
            typeRef,
            customLabelName,
            labelNameFromTypeRef,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildContextReceiver(init: FirContextReceiverBuilder.() -> Unit): FirContextReceiver {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirContextReceiverBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildContextReceiverCopy(original: FirContextReceiver, init: FirContextReceiverBuilder.() -> Unit): FirContextReceiver {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = FirContextReceiverBuilder()
    copyBuilder.source = original.source
    copyBuilder.typeRef = original.typeRef
    copyBuilder.customLabelName = original.customLabelName
    copyBuilder.labelNameFromTypeRef = original.labelNameFromTypeRef
    return copyBuilder.apply(init).build()
}
