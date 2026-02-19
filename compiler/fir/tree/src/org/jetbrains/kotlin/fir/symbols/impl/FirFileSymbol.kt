/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols.impl

import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.constant.EvaluatedConstTracker
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol

class FirFileSymbol : FirBasedSymbol<FirFile>(), EvaluatedConstTracker.Key {
    override fun toString(): String = "${this::class.simpleName} ${fir.name}"

    val sourceFile: KtSourceFile? get() = fir.sourceFile

    override fun asStringBasedKey(): EvaluatedConstTracker.Key.StringBased {
        return EvaluatedConstTracker.Key.StringBased(sourceFile?.path ?: "<no path>")
    }
}
