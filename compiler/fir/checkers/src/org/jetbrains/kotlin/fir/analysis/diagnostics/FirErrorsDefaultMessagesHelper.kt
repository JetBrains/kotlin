/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics

import org.jetbrains.kotlin.diagnostics.AbstractKtDiagnosticFactory
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import kotlin.reflect.full.memberProperties

fun KtDiagnosticFactoryToRendererMap.checkMissingMessages(objectWithErrors: Any) {
    for (property in objectWithErrors.javaClass.kotlin.memberProperties) {
        val factory = property.getter.call(objectWithErrors) as? AbstractKtDiagnosticFactory
        if (factory != null && !containsKey(factory)) {
            throw IllegalStateException("No default diagnostic renderer is provided for ${property.name}")
        }
    }
}