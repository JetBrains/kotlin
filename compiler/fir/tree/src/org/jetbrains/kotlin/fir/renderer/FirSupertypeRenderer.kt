/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.renderer

import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias

class FirSupertypeRenderer {
    internal lateinit var components: FirRendererComponents
    private val printer get() = components.printer
    private val visitor get() = components.visitor

    fun renderSupertypes(regularClass: FirRegularClass) {
        if (regularClass.superTypeRefs.isNotEmpty()) {
            printer.print(" : ")
            printer.renderSeparated(regularClass.superTypeRefs, visitor)
        }
    }

    fun renderTypeAliasExpansion(typeAlias: FirTypeAlias) {
        printer.print(" = ")
        typeAlias.expandedTypeRef.accept(visitor)
    }
}
