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

fun Set<ImplicitReceiverValue<*>>.singleWithoutDuplicatingContextReceiversOrNull(): ImplicitReceiverValue<*>? {
    return when {
        // KT-69102: we may encounter a bug with duplicated context receivers, and it wasn't
        // obvious how to fix it
        distinctBy { if (it.isContextReceiver) it.implicitScope else it }.count() == 1 -> this.firstOrNull()
        else -> null
    }
}

fun Set<ImplicitReceiverValue<*>>.ambiguityDiagnosticFor(labelName: String?): ConeSimpleDiagnostic {
    // This condition helps choose between an error diagnostic and a warning one to better
    // replicate the K1 behavior and avoid breaking changes.
    val areAlmostAllAnonymousFunctions = count { it.boundSymbol is FirAnonymousFunctionSymbol } >= size - 1

    val diagnostic = when {
        areAlmostAllAnonymousFunctions -> ConeSimpleDiagnostic("Clashing this@$labelName", DiagnosticKind.LabelNameClash)
        else -> ConeSimpleDiagnostic("Ambiguous this@$labelName", DiagnosticKind.AmbiguousLabel)
    }

    return diagnostic
}
