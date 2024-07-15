/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.scopes.impl.typeAliasForConstructor
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
internal abstract class FirRedeclarationPresenterBase {
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

    protected abstract fun StringBuilder.appendValueParameters(it: FirFunctionSymbol<*>)

    fun represent(declaration: FirBasedSymbol<*>): String? = when (declaration) {
        is FirNamedFunctionSymbol -> represent(declaration)
        is FirRegularClassSymbol -> represent(declaration)
        is FirTypeAliasSymbol -> represent(declaration)
        is FirVariableSymbol<*> -> represent(declaration)
        is FirConstructorSymbol -> {
            val container = declaration.typeAliasForConstructor?.classId ?: declaration.containingClassLookupTag()?.classId
            container?.let { represent(declaration, it) }
        }
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

    fun represent(it: FirConstructorSymbol, owner: FirClassLikeSymbol<*>) = represent(it, owner.classId)

    fun represent(it: FirConstructorSymbol, ownerClassId: ClassId) = buildString {
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
        appendRepresentation(ownerClassId)
        appendValueParameters(it)
    }
}

internal object FirRedeclarationPresenter : FirRedeclarationPresenterBase() {
    override fun StringBuilder.appendValueParameters(it: FirFunctionSymbol<*>) {
        append('(')
        append(it.valueParameterSymbols.size)
        append(')')
    }

    /**
     * Preserved for the deprecation cycle of KT-62746.
     */
    object OldVarargsCompatibilityPresenter : FirRedeclarationPresenterBase() {
        override fun StringBuilder.appendValueParameters(it: FirFunctionSymbol<*>) {
            append('(')
            it.valueParameterSymbols.forEach {
                appendRepresentation(it)
                append(',')
            }
            append(')')
        }

        private fun StringBuilder.appendRepresentation(it: FirValueParameterSymbol) {
            if (it.isVararg) {
                append("vararg ")
            }
        }
    }
}
