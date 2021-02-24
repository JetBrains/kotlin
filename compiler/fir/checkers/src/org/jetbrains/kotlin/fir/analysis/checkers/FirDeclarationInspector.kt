/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

/**
 * Provides representations for FirElement's.
 */
interface FirDeclarationPresenter {
    fun StringBuilder.appendRepresentation(it: FirElement) {
        append("NO_REPRESENTATION")
    }

    fun StringBuilder.appendRepresentation(it: ClassId) {
        append(it.packageFqName.asString())
        append('/')
        append(it.relativeClassName.asString())
    }

    fun StringBuilder.appendRepresentation(it: CallableId) {
        if (it.className != null) {
            append(it.packageName.asString())
            append('/')
            append(it.className)
            append('.')
            append(it.callableName)
        } else {
            append(it.packageName.asString())
            append('/')
            append(it.callableName)
        }
    }

    fun StringBuilder.appendRepresentation(it: ConeTypeProjection) {
        when (it) {
            ConeStarProjection -> {
                append('*')
            }
            is ConeKotlinTypeProjectionIn -> {
                append("in ")
                appendRepresentation(it.type)
            }
            is ConeKotlinTypeProjectionOut -> {
                append("out ")
                appendRepresentation(it.type)
            }
            is ConeKotlinType -> {
                appendRepresentation(it)
            }
        }
    }

    fun StringBuilder.appendRepresentation(it: ConeKotlinType) {
        when (it) {
            is ConeDefinitelyNotNullType -> {
                appendRepresentation(it.original)
                append(it.nullability.suffix)
            }
            is ConeClassErrorType -> {
                append("ERROR(")
                append(it.diagnostic.reason)
                append(')')
            }
            is ConeCapturedType -> {
                append(it.constructor.projection)
                append(it.nullability.suffix)
            }
            is ConeClassLikeType -> {
                appendRepresentation(it.lookupTag.classId)
                if (it.typeArguments.isNotEmpty()) {
                    append('<')
                    it.typeArguments.forEach { that ->
                        appendRepresentation(that)
                        append(',')
                    }
                    append('>')
                }
                append(it.nullability.suffix)
            }
            is ConeLookupTagBasedType -> {
                append(it.lookupTag.name)
                append(it.nullability.suffix)
            }
            is ConeIntegerLiteralType -> {
                append(it.value)
                append(it.nullability.suffix)
            }
            is ConeFlexibleType,
            is ConeIntersectionType,
            is ConeStubType -> {
                append("ERROR")
            }
        }
    }

    fun StringBuilder.appendRepresentation(it: FirTypeRef) {
        when (it) {
            is FirResolvedTypeRef -> appendRepresentation(it.type)
            is FirErrorTypeRef -> append("ERROR")
            else -> append("?")
        }
    }

    fun StringBuilder.appendRepresentation(it: FirTypeParameter) {
        append(it.name.asString())
        append(':')
        when (it.bounds.size) {
            0 -> {
            }
            1 -> {
                appendRepresentation(it.bounds[0])
            }
            else -> {
                val set = sortedSetOf<String>()
                it.bounds.forEach { that ->
                    set.add(buildString { appendRepresentation(that) })
                }
                set.forEach { that ->
                    append(that)
                    append(',')
                }
            }
        }
    }

    fun StringBuilder.appendRepresentation(it: FirValueParameter) {
        if (it.isVararg) {
            append("vararg ")
        }
        appendRepresentation(it.returnTypeRef)
    }

    fun represent(it: FirProperty) = buildString {
        append('[')
        it.receiverTypeRef?.let {
            appendRepresentation(it)
        }
        append(']')
        appendRepresentation(it.symbol.callableId)
    }

    fun represent(it: FirSimpleFunction) = buildString {
        append('<')
        it.typeParameters.forEach {
            appendRepresentation(it)
            append(',')
        }
        append('>')
        append('[')
        it.receiverTypeRef?.let {
            appendRepresentation(it)
        }
        append(']')
        if (it.isOperator) {
            append("operator ")
        }
        appendRepresentation(it.symbol.callableId)
        append('(')
        it.valueParameters.forEach {
            appendRepresentation(it)
            append(',')
        }
        append(')')
    }

    fun represent(it: FirTypeAlias) = buildString {
        append('[')
        append(']')
        appendRepresentation(it.symbol.classId)
    }

    fun represent(it: FirRegularClass) = buildString {
        append('[')
        append(']')
        appendRepresentation(it.symbol.classId)
    }
}

private class FirDefaultDeclarationPresenter : FirDeclarationPresenter

private val NO_NAME_PROVIDED = Name.special("<no name provided>")

// - see testEnumValuesValueOf.
// it generates a static function that has
// the same signature as the function defined
// explicitly.
// - see tests with `fun () {}`.
// you can't redeclare something that has no name.
private fun FirDeclaration.isCollectable() = when (this) {
    is FirSimpleFunction -> source?.kind !is FirFakeSourceElementKind && name != NO_NAME_PROVIDED
    is FirRegularClass -> name != NO_NAME_PROVIDED
    else -> true
}

/**
 * Collects FirDeclarations for further analysis.
 */
class FirDeclarationInspector(
    private val presenter: FirDeclarationPresenter = FirDefaultDeclarationPresenter()
) {
    val otherDeclarations = mutableMapOf<String, MutableList<FirDeclaration>>()
    val functionDeclarations = mutableMapOf<String, MutableList<FirSimpleFunction>>()

    fun collect(declaration: FirDeclaration) {
        if (!declaration.isCollectable()) {
            return
        }

        if (declaration is FirSimpleFunction) {
            return collectFunction(declaration)
        }

        val key = when (declaration) {
            is FirRegularClass -> presenter.represent(declaration)
            is FirTypeAlias -> presenter.represent(declaration)
            is FirProperty -> presenter.represent(declaration)
            else -> return
        }

        var value = otherDeclarations[key]

        if (value == null) {
            value = mutableListOf()
            otherDeclarations[key] = value
        }

        value.add(declaration)
    }

    private fun collectFunction(declaration: FirSimpleFunction) {
        val key = presenter.represent(declaration)
        var value = functionDeclarations[key]

        if (value == null) {
            value = mutableListOf()
            functionDeclarations[key] = value
        }

        value.add(declaration)
    }

    fun contains(declaration: FirDeclaration) = when (declaration) {
        is FirSimpleFunction -> presenter.represent(declaration) in functionDeclarations
        is FirRegularClass -> presenter.represent(declaration) in otherDeclarations
        is FirTypeAlias -> presenter.represent(declaration) in otherDeclarations
        is FirProperty -> presenter.represent(declaration) in otherDeclarations
        else -> false
    }
}