/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirArgumentList
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.expressions.FirEmptyArgumentList
import org.jetbrains.kotlin.fir.expressions.builder.FirCallBuilder
import org.jetbrains.kotlin.fir.expressions.impl.FirDelegatedConstructorCallImpl
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.impl.FirExplicitSuperReference
import org.jetbrains.kotlin.fir.references.impl.FirExplicitThisReference
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirDelegatedConstructorCallBuilder : FirCallBuilder, FirAnnotationContainerBuilder {
    override var source: FirSourceElement? = null
    override val annotations: MutableList<FirAnnotationCall> = mutableListOf()
    override var argumentList: FirArgumentList = FirEmptyArgumentList
    lateinit var constructedTypeRef: FirTypeRef
    var isThis: Boolean by kotlin.properties.Delegates.notNull<Boolean>()

    override fun build(): FirDelegatedConstructorCall {
        return FirDelegatedConstructorCallImpl(
            source,
            annotations,
            argumentList,
            constructedTypeRef,
            isThis,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildDelegatedConstructorCall(init: FirDelegatedConstructorCallBuilder.() -> Unit): FirDelegatedConstructorCall {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirDelegatedConstructorCallBuilder().apply(init).build()
}
