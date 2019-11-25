/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.resolve.ImplicitReceiverStack
import org.jetbrains.kotlin.fir.resolve.calls.ImplicitExtensionReceiverValue
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.name.Name

class FirTypeResolveScopeForBodyResolve(
    private val topLevelScopes: List<FirScope>,
    private val implicitReceiverStack: ImplicitReceiverStack,
    private val localScopes: List<FirScope>
) : FirScope() {
    override fun processClassifiersByName(
        name: Name,
        processor: (FirClassifierSymbol<*>) -> ProcessorAction
    ): ProcessorAction {
        for (scope in localScopes.asReversed()) {
            if (!scope.processClassifiersByName(name, processor)) {
                return ProcessorAction.STOP
            }
        }
        for (receiverValue in implicitReceiverStack.receiversAsReversed()) {
            if (receiverValue is ImplicitExtensionReceiverValue) continue
            if (receiverValue.implicitScope?.processClassifiersByName(name, processor) == ProcessorAction.STOP) {
                return ProcessorAction.STOP
            }
        }
        for (scope in topLevelScopes.asReversed()) {
            if (!scope.processClassifiersByName(name, processor)) {
                return ProcessorAction.STOP
            }
        }
        return ProcessorAction.NEXT
    }
}