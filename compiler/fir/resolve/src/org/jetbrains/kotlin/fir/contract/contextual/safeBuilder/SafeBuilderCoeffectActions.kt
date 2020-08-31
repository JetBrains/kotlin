/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.contract.contextual.safeBuilder

import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.fir.contracts.contextual.CoeffectContext
import org.jetbrains.kotlin.fir.contracts.contextual.CoeffectContextCleaner
import org.jetbrains.kotlin.fir.contracts.contextual.CoeffectContextProvider

class SafeBuilderCoeffectContextProvider(val action: SafeBuilderAction, val kind: EventOccurrencesRange) : CoeffectContextProvider {
    override val family = SafeBuilderCoeffectFamily

    override fun provideContext(context: CoeffectContext): CoeffectContext {
        if (context !is SafeBuilderCoeffectContext) throw AssertionError()
        val existingKind = context.actions[action] ?: EventOccurrencesRange.ZERO
        return SafeBuilderCoeffectContext(context.actions.put(action, existingKind + kind))
    }
}

class SafeBuilderCoeffectContextCleaner(val action: SafeBuilderAction) : CoeffectContextCleaner {
    override val family = SafeBuilderCoeffectFamily

    override fun cleanupContext(context: CoeffectContext): CoeffectContext {
        if (context !is SafeBuilderCoeffectContext) throw AssertionError()

        return if (action in context.actions) {
            SafeBuilderCoeffectContext(context.actions.remove(action))
        } else context
    }
}