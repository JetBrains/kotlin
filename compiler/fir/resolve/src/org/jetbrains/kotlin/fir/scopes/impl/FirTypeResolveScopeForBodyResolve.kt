/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.calls.ImplicitDispatchReceiverValue
import org.jetbrains.kotlin.fir.scopes.FirIterableScope
import org.jetbrains.kotlin.fir.scopes.FirScope

class FirTypeResolveScopeForBodyResolve(
    private val components: BodyResolveComponents
) : FirIterableScope() {
    override val scopes: Iterable<FirScope>
        get() = components.createCurrentScopeList()
}

fun BodyResolveComponents.createCurrentScopeList(): List<FirScope> =
    mutableListOf<FirScope>().apply {
        addAll(localScopes.asReversed())
        implicitReceiverStack.receiversAsReversed().mapNotNullTo(this) {
            (it as? ImplicitDispatchReceiverValue)?.implicitScope
        }

        addAll(typeParametersScopes.asReversed())
        addAll(fileImportsScope.asReversed())
    }
