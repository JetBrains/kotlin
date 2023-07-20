/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isOperator
import org.jetbrains.kotlin.fir.types.*
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

    private fun StringBuilder.appendRepresentation(it: FirValueParameter) {
        if (it.isVararg) {
            append("vararg ")
        }
    }

    private fun StringBuilder.appendRepresentationBeforeCallableId(it: FirCallableDeclaration) {
        repeat(it.contextReceivers.size) {
            append(',')
        }
        append('<')
        repeat(it.typeParameters.size) {
            append(',')
        }
        append('>')
        append('[')
        it.receiverParameter?.typeRef?.let {
            append(',')
        }
        append(']')
    }

    private fun StringBuilder.appendValueParameters(it: FirSimpleFunction) {
        append('(')
        it.valueParameters.forEach {
            appendRepresentation(it)
            append(',')
        }
        append(')')
    }

    fun represent(declaration: FirDeclaration): String? = when (declaration) {
        is FirSimpleFunction -> represent(declaration)
        is FirRegularClass -> represent(declaration)
        is FirTypeAlias -> represent(declaration)
        is FirProperty -> represent(declaration)
        else -> null
    }

    fun represent(it: FirSimpleFunction) = buildString {
        appendRepresentationBeforeCallableId(it)
        if (it.isOperator) {
            append("operator ")
        }
        appendRepresentation(it.symbol.callableId)
        appendValueParameters(it)
    }


    fun represent(it: FirVariable) = buildString {
        appendRepresentationBeforeCallableId(it)
        appendRepresentation(it.symbol.callableId)
    }

    fun represent(it: FirTypeAlias) = representClassLike(it)
    fun represent(it: FirRegularClass) = representClassLike(it)

    private fun representClassLike(it: FirClassLikeDeclaration) = buildString {
        append('<')
        append('>')
        append('[')
        append(']')
        appendRepresentation(it.symbol.classId)
    }

    fun represent(it: FirConstructor, owner: FirRegularClass) = buildString {
        repeat(it.contextReceivers.size) {
            append(',')
        }
        append('<')
        repeat(it.typeParameters.size) {
            append(',')
        }
        append('>')
        append('[')
        append(']')
        appendRepresentation(owner.symbol.classId)
        append('(')
        it.valueParameters.forEach {
            appendRepresentation(it)
            append(',')
        }
        append(')')
    }
}
