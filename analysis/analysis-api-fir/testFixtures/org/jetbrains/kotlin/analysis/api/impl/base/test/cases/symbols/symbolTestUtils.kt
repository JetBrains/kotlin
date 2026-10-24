/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.javaInterop.containingJvmClassName
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import kotlin.reflect.full.createType
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.withNullability
import kotlin.reflect.jvm.javaField

context(_: KaSession)
internal fun checkContainingFileSymbol(
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

context(_: KaSession)
internal fun checkContainingJvmClassName(
    ktFile: KtFile,
    ktClass: KtClassOrObject?,
    symbol: KaCallableSymbol,
    testServices: TestServices
) {
    fun KaCallableSymbol.computeExpectedJvmClassName(): String? = when {
        this is KaParameterSymbol -> (containingSymbol as? KaFunctionSymbol)?.computeExpectedJvmClassName()
        this.isLocal -> null
        ktClass != null -> ktClass.getClassId()?.asFqNameString() // Member
        else -> ktFile.javaFileFacadeFqName.asString() // Top-level
    }

    if (ktFile.isScript()) return
    val expectedClassName = symbol.computeExpectedJvmClassName()
    val actualClassName = symbol.containingJvmClassName
    testServices.assertions.assertEquals(expectedClassName, actualClassName) {
        "Invalid JvmClassName for $symbol, expected $expectedClassName but $actualClassName found"
    }
}

/**
 * Some K2 implementations of [KaSymbol] is backed by some [PsiElement],
 * so they may implement some API on top of PSI, FirSymbols or both of them.
 *
 * FirSymbol-based implementation is the source of truth, so the PSI-based implementation
 * exists to cover simple cases.
 *
 * As most of the symbols have the underlying PSI element, it is crucial to
 * have consistent implementation for PSI-based and FirSymbol-based symbols.
 */
internal fun KaSymbol.dropBackingPsi() {
    val interfaceInstance = Class.forName("org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirPsiSymbol")
    val symbolType = KaSymbol::class.createType()

    val thisClass = this::class
    for (property in thisClass.declaredMemberProperties) {
        // Some symbols may have owning symbols, so they should be invalidated as well
        if (!property.name.startsWith("owning") || !property.returnType.isSubtypeOf(symbolType)) continue

        val symbol = property.getter.call(this) as KaSymbol
        symbol.dropBackingPsi()
    }

    if (!interfaceInstance.isInstance(this)) return

    when (thisClass.simpleName) {
        // Those classes are PSI-based only, so they have FirSymbol only for the compatibility with other classes
        "KaFirPsiJavaClassSymbol",
        "KaFirPsiJavaTypeParameterSymbol",
            -> return

        // There classes depend on the property PSI. The owning property is already invalidated above
        "KaFirDefaultPropertyGetterSymbol",
        "KaFirDefaultPropertySetterSymbol",
        "KaFirPropertyGetterSymbol",
        "KaFirPropertySetterSymbol",
            -> return
    }

    val property = thisClass.memberProperties.single { it.name == "backingPsi" }
    val returnType = property.returnType
    if (!returnType.isSubtypeOf(PsiElement::class.createType().withNullability(true))) {
        error("Unexpected return type '$returnType' for '${this::class.simpleName}' class")
    }

    val field = property.javaField ?: error("Backing field is not found")
    field.isAccessible = true

    // Drop backing PSI to trigger non-psi implementation
    field.set(this, null)
}
