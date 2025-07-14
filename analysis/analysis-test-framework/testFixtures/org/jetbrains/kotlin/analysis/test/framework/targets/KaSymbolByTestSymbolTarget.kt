/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.targets

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.psi.KtFile
import java.nio.file.Path

/**
 * Creates a [TestSymbolTarget] from the test data file at [testDataPath] and resolves all [KaSymbol]s from it using the
 * [KaSymbolTestSymbolTargetResolver].
 *
 * @param contextFile The [KtFile] from which the [TestSymbolTarget] should be resolved. See [TestSymbolTarget.create].
 */
fun KaSession.getTestTargetSymbols(testDataPath: Path, contextFile: KtFile): List<KaSymbol> {
    val target = TestSymbolTarget.parse(testDataPath, contextFile)
    return KaSymbolTestSymbolTargetResolver(this).resolveTarget(target)
}

/**
 * Creates a [TestSymbolTarget] from the test data file at [testDataPath] and resolve a single [KaSymbol] of type [S] from it using the
 * [KaSymbolTestSymbolTargetResolver]. If there is no such single symbol of that type, the function throws an error.
 *
 * @param contextFile The [KtFile] from which the [TestSymbolTarget] should be resolved. [contextFile] must be from the same module as the
 *  [KaSession].
 */
inline fun <reified S : KaSymbol> KaSession.getSingleTestTargetSymbolOfType(testDataPath: Path, contextFile: KtFile): S {
    val symbols = getTestTargetSymbols(testDataPath, contextFile)
    return symbols.singleOrNull() as? S
        ?: error("Expected a single target `${S::class.simpleName}` to be specified, but found the following symbols: $symbols")
}
