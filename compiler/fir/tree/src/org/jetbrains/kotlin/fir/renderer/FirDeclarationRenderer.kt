/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.renderer

import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.isCatchParameter
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

open class FirDeclarationRenderer(
    private val localVariablePrefix: String = "l",
) {

    internal lateinit var components: FirRendererComponents
    protected val printer get() = components.printer
    private val resolvePhaseRenderer get() = components.resolvePhaseRenderer
    private val typeRenderer get() = components.typeRenderer

    fun render(declaration: FirDeclaration) {
        renderPhaseAndAttributes(declaration)
        if (declaration is FirConstructor) {
            declaration.dispatchReceiverType?.let {
                typeRenderer.render(it)
                printer.print(".")
            }
            if (declaration is FirErrorPrimaryConstructor) {
                printer.print("error_")
            }
            printer.print("constructor")
            return
        }
        printer.print(
            when (declaration) {
                is FirRegularClass -> declaration.classKind.name.toLowerCaseAsciiOnly().replace("_", " ")
                is FirTypeAlias -> "typealias"
                is FirAnonymousFunction -> (declaration.label?.let { "${it.name}@" } ?: "") + "fun"
                is FirSimpleFunction -> "fun"
                is FirProperty -> {
                    if (declaration.isCatchParameter == true) {
                        ""
                    } else {
                        val prefix = if (declaration.isLocal) localVariablePrefix else ""
                        prefix + if (declaration.isVal) "val" else "var"
                    }
                }
                is FirPropertyAccessor -> if (declaration.isGetter) "get" else "set"
                is FirField -> "field"
                is FirEnumEntry -> "enum entry"
                else -> "unknown"
            }
        )
    }

    internal fun renderPhaseAndAttributes(declaration: FirDeclaration) {
        resolvePhaseRenderer?.render(declaration)
        with(declaration) {
            renderDeclarationAttributes()
        }
    }

    protected open fun FirDeclaration.renderDeclarationAttributes() {
    }
}
