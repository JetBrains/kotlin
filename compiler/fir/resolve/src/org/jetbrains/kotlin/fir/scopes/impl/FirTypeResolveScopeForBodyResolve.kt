/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.resolve.ImplicitReceiverStack
import org.jetbrains.kotlin.fir.resolve.calls.ImplicitExtensionReceiverValue
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.name.Name

class FirTypeResolveScopeForBodyResolve(
    private val topLevelScopes: List<FirScope>,
    private val implicitReceiverStack: ImplicitReceiverStack,
    private val localScopes: List<FirScope>
) : FirScope() {
    override fun processClassifiersByName(
        name: Name,
        processor: (FirClassifierSymbol<*>) -> Unit
    ) {
        for (scope in localScopes.asReversed()) {
            scope.processClassifiersByName(name, processor)
        }
        for (receiverValue in implicitReceiverStack.receiversAsReversed()) {
            if (receiverValue is ImplicitExtensionReceiverValue) continue
            receiverValue.implicitScope?.processClassifiersByName(name, processor)
        }
        for (scope in topLevelScopes.asReversed()) {
            scope.processClassifiersByName(name, processor)
        }
    }
}