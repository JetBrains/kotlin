/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.fir.declarations.utils.isOperator
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId

/**
 * Provides representations for FirElements consisting of declaration name and shape of parameters,
 * i.e., number of context receivers, receivers, type parameters, and parameters.
 *
 * Elements that are potential redeclarations have the same representations, e.g., properties without receivers and classes.
 */
internal object FirRedeclarationPresenter {
    private fun StringBuilder.appendRepresentation(it: ClassId) {
        append(it.packageFqName.asString())
        append('/')
        append(it.relativeClassName.asString())
    }

    private fun StringBuilder.appendRepresentation(it: CallableId) {
        append(it.packageName.asString())
        append('/')
        if (it.className != null) {
            append(it.className)
            append('.')
        }
        append(it.callableName)
    }

    private fun StringBuilder.appendRepresentation(it: FirValueParameterSymbol) {
        if (it.isVararg) {
            append("vararg ")
        }
    }

    private fun StringBuilder.appendRepresentationBeforeCallableId(it: FirCallableSymbol<*>) {
        repeat(it.resolvedContextReceivers.size) {
            append(',')
        }
        append('<')
        repeat(it.typeParameterSymbols.size) {
            append(',')
        }
        append('>')
        append('[')
        it.receiverParameter?.typeRef?.let {
            append(',')
        }
        append(']')
    }

    private fun StringBuilder.appendValueParameters(it: FirNamedFunctionSymbol) {
        append('(')
        it.valueParameterSymbols.forEach {
            appendRepresentation(it)
            append(',')
        }
        append(')')
    }

    fun represent(declaration: FirBasedSymbol<*>): String? = when (declaration) {
        is FirNamedFunctionSymbol -> represent(declaration)
        is FirRegularClassSymbol -> represent(declaration)
        is FirTypeAliasSymbol -> represent(declaration)
        is FirPropertySymbol -> represent(declaration)
        else -> null
    }

    fun represent(it: FirNamedFunctionSymbol) = buildString {
        appendRepresentationBeforeCallableId(it)
        appendRepresentation(it.callableId)
        appendValueParameters(it)
    }


    fun represent(it: FirVariableSymbol<*>) = buildString {
        appendRepresentationBeforeCallableId(it)
        appendRepresentation(it.callableId)

        if (it is FirFieldSymbol) {
            append("#f")
        }
    }

    fun represent(it: FirTypeAliasSymbol) = representClassLike(it)
    fun represent(it: FirRegularClassSymbol) = representClassLike(it)

    private fun representClassLike(it: FirClassLikeSymbol<*>) = buildString {
        append('<')
        append('>')
        append('[')
        append(']')
        appendRepresentation(it.classId)
    }

    fun represent(it: FirConstructorSymbol, owner: FirClassLikeSymbol<*>) = buildString {
        repeat(it.resolvedContextReceivers.size) {
            append(',')
        }
        append('<')
        repeat(it.typeParameterSymbols.size) {
            append(',')
        }
        append('>')
        append('[')
        append(']')
        appendRepresentation(owner.classId)
        append('(')
        it.valueParameterSymbols.forEach {
            appendRepresentation(it)
            append(',')
        }
        append(')')
    }
}
