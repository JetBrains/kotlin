/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics

interface GenericDiagnostics<T : UnboundDiagnostic> : Iterable<T> {
    fun all(): Collection<T>

    fun isEmpty(): Boolean = all().isEmpty()

    override fun iterator(): Iterator<T> = all().iterator()
}