/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.declarations.utils.isCompanionExtension
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol

enum class CompanionExtensionPolicy(
    private val acceptsCompanionExtension: Boolean,
) {
    OnlyCompanionExtensions(true),
    NoCompanionExtensions(false),
    ;

    fun accepts(candidate: FirCallableSymbol<*>): Boolean {
        return candidate.isCompanionExtension == acceptsCompanionExtension
    }
}