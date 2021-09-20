/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.tower

import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.resolve.BindingTrace

class TrackingBindingTrace(val trace: BindingTrace) : BindingTrace by trace {
    var reported: Boolean = false

    override fun report(diagnostic: Diagnostic) {
        if (bindingContext.diagnostics.noSuppression().forElement(diagnostic.psiElement).any { it == diagnostic }) return

        trace.report(diagnostic)
        reported = true
    }

    fun markAsReported() {
        reported = true
    }
}