/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.resolve.calls.ImplicitDispatchReceiverValue
import org.jetbrains.kotlin.fir.resolve.calls.ImplicitReceiverValue
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol

abstract class ImplicitReceiverStack : Iterable<ImplicitReceiverValue<*>> {
    abstract operator fun get(name: String?): Set<ImplicitReceiverValue<*>>

    abstract fun lastDispatchReceiver(): ImplicitDispatchReceiverValue?
    abstract fun lastDispatchReceiver(lookupCondition: (ImplicitReceiverValue<*>) -> Boolean): ImplicitDispatchReceiverValue?
    abstract fun receiversAsReversed(): List<ImplicitReceiverValue<*>>
}

inline fun Set<ImplicitReceiverValue<*>>.ifMoreThanOne(
    block: (Boolean) -> ImplicitReceiverValue<*>?,
) = when {
    // KT-69102: we may encounter a bug with duplicated context receivers, and it wasn't
    // obvious how to fix it
    distinctBy { if (it.isContextReceiver) it.implicitScope else it }.count() == 1 -> this.firstOrNull()
    else -> block(all { it.boundSymbol is FirAnonymousFunctionSymbol })
}

fun clashingLabelDiagnostic(labelName: String?, areOnlyAnonymousFunctions: Boolean): ConeSimpleDiagnostic {
    return when {
        areOnlyAnonymousFunctions -> ConeSimpleDiagnostic("Clashing this@$labelName", DiagnosticKind.LabelNameClash)
        else -> ConeSimpleDiagnostic("Ambiguous this@$labelName", DiagnosticKind.AmbiguousLabel)
    }
}
