/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch")

package org.jetbrains.kotlin.diagnostics

/**
 * This methods should me in `org.jetbrains.kotlin.diagnostics` package
 *   because of `DiagnosticFactory.setName` is package private
 */
fun DiagnosticFactory<*>.initializeName(name: String) {
    this.name = name
}