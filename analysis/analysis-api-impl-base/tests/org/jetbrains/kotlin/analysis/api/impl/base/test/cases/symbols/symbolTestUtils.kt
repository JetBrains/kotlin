/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFileSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaSymbolKind
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

internal fun KaSession.checkContainingFileSymbol(
    ktFileSymbol: KaFileSymbol,
    symbol: KaSymbol,
    testServices: TestServices
) {
    if (symbol.origin != KaSymbolOrigin.SOURCE) return
    val containingFileSymbol = symbol.getContainingFileSymbol()
    testServices.assertions.assertEquals(ktFileSymbol, containingFileSymbol) {
        "Invalid file for $symbol, expected $ktFileSymbol but $containingFileSymbol found"
    }
}

internal fun KaSession.checkContainingJvmClassName(
    ktFile: KtFile,
    ktClass: KtClassOrObject?,
    symbol: KaCallableSymbol,
    testServices: TestServices
) {
    if (ktFile.isScript()) return
    val expectedClassName = when {
        symbol.symbolKind == KaSymbolKind.LOCAL ->
            null
        ktClass != null ->
            // member
            ktClass.getClassId()?.asFqNameString()
        else ->
            // top-level
            ktFile.javaFileFacadeFqName.asString()
    }
    val actualClassName = symbol.getContainingJvmClassName()
    testServices.assertions.assertEquals(expectedClassName, actualClassName) {
        "Invalid JvmClassName for $symbol, expected $expectedClassName but $actualClassName found"
    }
}