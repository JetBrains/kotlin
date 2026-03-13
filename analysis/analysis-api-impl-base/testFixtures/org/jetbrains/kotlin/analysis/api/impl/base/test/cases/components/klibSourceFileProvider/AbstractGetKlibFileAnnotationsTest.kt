/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.klibSourceFileProvider

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.klibFileAnnotations
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.annotations.TestAnnotationRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol

/**
 * Reads through the declarations provided in the .klib and renders their `klibFileAnnotationClassIds`.
 */
abstract class AbstractGetKlibFileAnnotationsTest : AbstractKlibSourceFileProviderTest() {
    context(session: KaSession)
    private fun KaDeclarationSymbol.renderAnnotations(): String =
        klibFileAnnotations
            ?.let { TestAnnotationRenderer.renderAnnotations(session, it, prefix = "klibFileAnnotations") }
            ?: "klibFileAnnotations: null"

    context(_: KaSession)
    override fun renderTopLevelClass(classSymbol: KaClassSymbol): String =
        "Classifier: ${classSymbol.classId}; ${classSymbol.renderAnnotations()}"

    context(_: KaSession)
    override fun renderTopLevelCallable(symbol: KaCallableSymbol): String {
        return "Callable: ${symbol.callableId}; ${symbol.renderAnnotations()}"
    }
}
