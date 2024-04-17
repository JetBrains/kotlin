/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtFileSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbolOrigin
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolKind
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

context(KtAnalysisSession)
internal fun checkContainingFileSymbol(
    ktFileSymbol: KtFileSymbol,
    symbol: KtSymbol,
    testServices: TestServices
) {
    if (symbol.origin != KtSymbolOrigin.SOURCE) return
    val containingFileSymbol = symbol.getContainingFileSymbol()
    testServices.assertions.assertEquals(ktFileSymbol, containingFileSymbol) {
        "Invalid file for $symbol, expected $ktFileSymbol but $containingFileSymbol found"
    }
}

context(KtAnalysisSession)
internal fun checkContainingJvmClassName(
    ktFile: KtFile,
    ktClass: KtClassOrObject?,
    symbol: KtCallableSymbol,
    testServices: TestServices
) {
    if (ktFile.isScript()) return
    val expectedClassName = when {
        symbol.symbolKind == KtSymbolKind.LOCAL ->
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