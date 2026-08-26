/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol

/**
 * The internal bridge for utilities sharing.
 */
@KaImplementationDetail
interface KaInternalsLightClassBridge {
    /**
     * Applies [JvmName] and `internal` mangling to [defaultName].
     *
     * @return `null` if the name is mangled because of value classes, as such a suffix is out of the endpoint's scope
     */
    context(_: KaSession)
    fun computeJavaMethodName(symbol: KaCallableSymbol, defaultName: String): String?
}
