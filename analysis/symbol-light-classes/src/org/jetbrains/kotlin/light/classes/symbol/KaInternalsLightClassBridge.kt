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
     * @param ignoreInlineClassMangling whether to compute the name as if inline classes did not require mangling
     * @return the computed Java method name, or `null` if inline-class mangling is required and
     * [ignoreInlineClassMangling] is `false`
     */
    context(_: KaSession)
    fun computeJavaMethodName(symbol: KaCallableSymbol, defaultName: String, ignoreInlineClassMangling: Boolean): String?
}
