/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch")

package org.jetbrains.kotlin.diagnostics

import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import kotlin.reflect.full.memberProperties

fun FirErrors.registerExternalFactories() {
    val klass = FirErrors::class
    for (property in klass.memberProperties) {
        val factory = property.get(this) as? DiagnosticFactory<*> ?: continue
        factory.name = property.name
    }
}