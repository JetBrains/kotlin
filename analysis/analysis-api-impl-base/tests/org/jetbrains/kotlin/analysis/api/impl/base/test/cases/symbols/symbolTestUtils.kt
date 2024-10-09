/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFileSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.analysis.api.symbols.isLocal
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
    val containingFileSymbol = symbol.containingFile
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
    fun KaCallableSymbol.computeExpectedJvmClassName(): String? {
        return when {
            this is KaParameterSymbol -> (containingSymbol as KaFunctionSymbol).computeExpectedJvmClassName()
            this.isLocal -> null
            ktClass != null -> ktClass.getClassId()?.asFqNameString() // Member
            else -> ktFile.javaFileFacadeFqName.asString() // Top-level
        }
    }

    if (ktFile.isScript()) return
    val expectedClassName = symbol.computeExpectedJvmClassName()
    val actualClassName = symbol.containingJvmClassName
    testServices.assertions.assertEquals(expectedClassName, actualClassName) {
        "Invalid JvmClassName for $symbol, expected $expectedClassName but $actualClassName found"
    }
}