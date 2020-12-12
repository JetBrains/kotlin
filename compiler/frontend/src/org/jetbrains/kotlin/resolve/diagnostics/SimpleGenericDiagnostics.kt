/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.diagnostics

import org.jetbrains.kotlin.diagnostics.UnboundDiagnostic
import org.jetbrains.kotlin.diagnostics.GenericDiagnostics
import java.util.ArrayList

open class SimpleGenericDiagnostics<T : UnboundDiagnostic>(diagnostics: Collection<T>) : GenericDiagnostics<T> {
    //copy to prevent external change
    private val diagnostics = ArrayList(diagnostics)

    override fun all() = diagnostics
}
