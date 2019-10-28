/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.diagnostics

import kotlin.reflect.KProperty

object ConeDiagnostics {
    val INFIX_MODIFIER_REQUIRED = ConeDiagnosticFactory()
    val INAPPLICABLE_INFIX_MODIFIER = ConeDiagnosticFactory()

    init {
        ConeDiagnostics::class.members.filterIsInstance<KProperty<*>>().forEach { property ->
            val factory = property.getter.call(this) as? ConeDiagnosticFactory
            factory?.name = property.name
        }
    }
}