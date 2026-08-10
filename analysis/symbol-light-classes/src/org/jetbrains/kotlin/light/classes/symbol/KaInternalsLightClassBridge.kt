/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol

/**
 * The internal bridge for utilities sharing.
 */
@KaImplementationDetail
interface KaInternalsLightClassBridge {
    /**
     * The declaration that owns the JVM method for [symbol], or `null` if the method is placed into a file facade class.
     *
     * For a property accessor, the owner of the property is used, as an accessor is never owned by its property on the JVM.
     */
    context(_: KaSession)
    fun jvmMethodOwner(symbol: KaCallableSymbol): KaDeclarationSymbol?

    /**
     * Whether the JVM name of [symbol] is mangled because of value classes.
     *
     * The suffix is either a hash of the signature, as in `classFunInParameter-5lyY9Q4`, or `impl` for a member of a value class,
     * as in `funWithoutParameters-impl`.
     */
    context(_: KaSession)
    fun hasMangledNameDueToValueClasses(symbol: KaCallableSymbol): Boolean
}
