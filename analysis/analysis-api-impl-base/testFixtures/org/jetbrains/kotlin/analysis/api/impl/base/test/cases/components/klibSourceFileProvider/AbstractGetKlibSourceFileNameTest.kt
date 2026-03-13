/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.klibSourceFileProvider

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.klibSourceFileName
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol

/**
 * Reads through the declarations provided in the .klib and renders their `klibSourceFile`
 */
abstract class AbstractGetKlibSourceFileNameTest : AbstractKlibSourceFileProviderTest() {
    context(_: KaSession)
    override fun renderTopLevelClass(classSymbol: KaClassSymbol): String =
        "Classifier: ${classSymbol.classId}; klibSourceFile: ${classSymbol.klibSourceFileName}"

    context(_: KaSession)
    override fun renderTopLevelCallable(symbol: KaCallableSymbol): String {
        return "Callable: ${symbol.callableId}; klibSourceFile: ${symbol.klibSourceFileName}"
    }
}