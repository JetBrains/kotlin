/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isOperator
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId

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
            is ConeKotlinTypeConflictingProjection -> {}
        }
    }

    fun StringBuilder.appendRepresentation(it: ConeKotlinType) {
        when (it) {
            is ConeDefinitelyNotNullType -> {
                appendRepresentation(it.original)
                append(it.nullability.suffix)
            }
            is ConeErrorType -> {
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
            is ConeIntegerLiteralConstantType -> {
                append(it.value)
                append(it.nullability.suffix)
            }
            is ConeIntegerConstantOperatorType -> {
                append("IOT")
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

    fun represent(it: FirVariable) = buildString {
        append('[')
        it.receiverParameter?.typeRef?.let {
            appendRepresentation(it)
        }
        append(']')
        appendRepresentation(it.symbol.callableId)
    }

    fun StringBuilder.appendOperatorTag(it: FirSimpleFunction) {
        if (it.isOperator) {
            append("operator ")
        }
    }

    fun represent(it: FirSimpleFunction) = buildString {
        it.contextReceivers.forEach {
            appendRepresentation(it)
            append(',')
        }
        append('<')
        it.typeParameters.forEach {
            appendRepresentation(it)
            append(',')
        }
        append('>')
        append('[')
        it.receiverParameter?.typeRef?.let {
            appendRepresentation(it)
        }
        append(']')
        appendOperatorTag(it)
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

    fun represent(it: FirConstructor, owner: FirRegularClass) = buildString {
        it.contextReceivers.forEach {
            appendRepresentation(it)
            append(',')
        }
        append('<')
        it.typeParameters.forEach {
            appendRepresentation(it)
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