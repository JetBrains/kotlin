/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.collectEnumEntries
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildErrorNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.references.resolved
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedNameError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedSymbolError
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol

@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
annotation class UnsafeExpressionUtility

/**
 * Returns the [FirReference] of this [FirElement], if available.
 * The reference is resolved in the context of a use-site [session], which may be required to find a symbol for an ID-based expression.
 */
fun FirElement.toReference(session: FirSession): FirReference? {
    return when (this) {
        is FirExpression -> toReferenceImpl(session)
        is FirVariableAssignment -> calleeReference
        is FirResolvable -> calleeReference
        else -> null
    }
}

/**
 * Returns the [FirReference] of this [FirExpression], if available.
 * The reference is resolved in the context of a use-site [session], which may be required to find a symbol for an ID-based expression.
 */
fun FirExpression.toReference(session: FirSession): FirReference? {
    return toReferenceImpl(session)
}

/**
 * This utility won't return proper reference for `FirEnumEntryDeserializedAccessExpression`,
 *   so use only if you are really sure that it be never called on this node
 *
 * In most cases it's better to use safe [toReference] methods with session parameter.
 */
@UnsafeExpressionUtility
fun FirExpression.toReferenceUnsafe(): FirReference? {
    return toReferenceImpl(session = null)
}

private fun FirExpression.toReferenceImpl(session: FirSession?): FirReference? {
    return when (this) {
        is FirEnumEntryDeserializedAccessExpression -> {
            requireNotNull(session)
            toReference(session)
        }
        is FirWrappedArgumentExpression -> expression.toResolvedCallableReferenceImpl(session)
        is FirSmartCastExpression -> originalExpression.toReferenceImpl(session)
        is FirDesugaredAssignmentValueReferenceExpression -> expressionRef.value.toReferenceImpl(session)
        is FirResolvable -> calleeReference
        else -> null
    }
}

fun FirEnumEntryDeserializedAccessExpression.toReference(session: FirSession): FirReference {
    fun createErrorReference(diagnostic: ConeDiagnostic): FirErrorNamedReference {
        return buildErrorNamedReference {
            this.diagnostic = diagnostic
        }
    }

    val enumSymbol = this.resolvedType.toRegularClassSymbol(session)
        ?: return createErrorReference(ConeUnresolvedSymbolError(resolvedType.classId!!))
    val enumEntrySymbol = enumSymbol.collectEnumEntries().firstOrNull { it.name == enumEntryName }
        ?: return createErrorReference(ConeUnresolvedNameError(enumEntryName))

    return buildResolvedNamedReference {
        name = enumEntryName
        resolvedSymbol = enumEntrySymbol
    }
}

val FirVariableAssignment.calleeReference: FirReference?
    get() {
        // non-nullable session for `toReferenceImpl` is needed only for `FirEnumEntryDeserializedAccessExpression` value,
        // which never can appear in the lhs of variable assignment
        return lValue.toReferenceImpl(session = null)
    }

// --------------------------------------------------------------------------------------------------------

fun FirExpression.toResolvedCallableReference(session: FirSession): FirResolvedNamedReference? {
    return toResolvedCallableReferenceImpl(session)
}

fun FirResolvable.toResolvedCallableReference(): FirResolvedNamedReference? {
    return calleeReference.resolved
}

/**
 * This utility won't return proper reference for `FirEnumEntryDeserializedAccessExpression`,
 *   so use only if you are really sure that it be never called on this node
 *
 * In most cases it's better to use safe [toResolvedCallableReference] methods with session parameter
 */
@UnsafeExpressionUtility
fun FirExpression.toResolvedCallableReferenceUnsafe(): FirResolvedNamedReference? {
    return toResolvedCallableReferenceImpl(session = null)
}

fun FirExpression.toResolvedCallableReferenceImpl(session: FirSession?): FirResolvedNamedReference? {
    return toReferenceImpl(session)?.resolved
}

// --------------------------------------------------------------------------------------------------------

fun FirExpression.toResolvedCallableSymbol(session: FirSession): FirCallableSymbol<*>? {
    return toResolvedCallableReference(session)?.resolvedSymbol as? FirCallableSymbol<*>?
}

fun FirResolvable.toResolvedCallableSymbol(): FirCallableSymbol<*>? {
    return toResolvedCallableReference()?.resolvedSymbol as? FirCallableSymbol<*>
}

/**
 * This utility won't return proper reference for `FirEnumEntryDeserializedAccessExpression`,
 *   so use only if you are really sure that it be never called on this node
 *
 * In most cases it's better to use safe [toResolvedCallableSymbol] methods with session parameter
 */
@UnsafeExpressionUtility
fun FirExpression.toResolvedCallableSymbolUnsafe(): FirCallableSymbol<*>? {
    return toResolvedCallableReferenceUnsafe()?.resolvedSymbol as? FirCallableSymbol<*>?
}
