/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.utils

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirComponentCall
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirReplExpressionReference
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.fir.types.isNullableAny
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions

val FirTypeAlias.expandedConeType: ConeClassLikeType? get() = expandedTypeRef.coneTypeSafe()

val FirClassLikeDeclaration.classId: ClassId
    get() = symbol.classId

val FirClass.superConeTypes: List<ConeClassLikeType> get() = superTypeRefs.mapNotNull { it.coneTypeSafe() }

@OptIn(DirectDeclarationsAccess::class)
val FirClass.anonymousInitializers: List<FirAnonymousInitializer>
    get() = declarations.filterIsInstance<FirAnonymousInitializer>()

@DirectDeclarationsAccess
val FirClass.delegateFields: List<FirField>
    get() = declarations.filterIsInstance<FirField>().filter { it.isSynthetic }

inline val FirDeclaration.isJava: Boolean
    get() = origin is FirDeclarationOrigin.Java
inline val FirDeclaration.isFromLibrary: Boolean
    get() = origin == FirDeclarationOrigin.Library || origin == FirDeclarationOrigin.Java.Library
inline val FirDeclaration.isPrecompiled: Boolean
    get() = origin == FirDeclarationOrigin.Precompiled
inline val FirDeclaration.isSynthetic: Boolean
    get() = origin is FirDeclarationOrigin.Synthetic

/**
 * An extension to determine locality for a general [FirDeclaration].
 *
 * A FIR declaration is non-local iff all its parents are non-local, or it's a [FirFile].
 */
val FirDeclaration.isLocal: Boolean
    get() = when (this) {
        is FirFile -> false
        is FirMemberDeclaration -> isLocal
        else -> true
    }

/**
 * `true` when [FirCallableDeclaration.receiverParameter] is not `null`.
 *
 * Use [isInstanceExtension] to check for extensions excluding companion extensions
 * or [isCompanionExtension] to check for companion extensions.
 */
val FirCallableDeclaration.isExtension: Boolean get() = receiverParameter != null

/**
 * `true` when [FirCallableDeclaration.receiverParameter] is not `null` and the declaration is not static,
 * meaning it's not a companion extension.
 *
 * As of writing this, companion block members cannot be extensions.
 *
 * @see isCompanionExtension
 */
val FirCallableDeclaration.isInstanceExtension: Boolean get() = isExtension && !isStatic

val FirBasedSymbol<*>.isMemberDeclaration: Boolean
    // Accessing `fir` is ok, because we don't really use it
    get() = fir is FirMemberDeclaration

val FirBasedSymbol<*>.memberDeclarationNameOrNull: Name?
    // Accessing `fir` is ok, because `nameOrSpecialName` only accesses names
    get() = (fir as? FirMemberDeclaration)?.nameOrSpecialName

val FirMemberDeclaration.nameOrSpecialName: Name
    get() = when (this) {
        is FirCallableDeclaration -> symbol.name
        is FirClassLikeDeclaration -> classId.shortClassName
    }

fun FirBasedSymbol<*>.asMemberDeclarationResolvedTo(phase: FirResolvePhase): FirMemberDeclaration? {
    return (fir as? FirMemberDeclaration)?.also {
        lazyResolveToPhase(phase)
    }
}

val FirNamedFunctionSymbol.isMethodOfAny: Boolean
    get() {
        if (isExtension || hasContextParameters) return false
        return when (name) {
            OperatorNameConventions.EQUALS -> valueParameterSymbols.singleOrNull()?.resolvedReturnType?.isNullableAny == true
            OperatorNameConventions.HASH_CODE, OperatorNameConventions.TO_STRING -> fir.valueParameters.isEmpty()
            else -> false
        }
    }

val FirConstructorSymbol.isErrorPrimaryConstructor: Boolean get() = fir is FirErrorPrimaryConstructor

fun FirStatement.isDestructuredParameter(): Boolean = this is FirVariable && getDestructuredParameter() != null

fun FirVariable.getDestructuredParameter(): FirValueParameterSymbol? {
    val initializer = initializer
    if (initializer !is FirComponentCall) return null
    if (initializer.source?.kind !is KtFakeSourceElementKind.DesugaredComponentFunctionCall) return null
    val receiver = initializer.dispatchReceiver ?: initializer.extensionReceiver ?: return null
    if (receiver !is FirPropertyAccessExpression) return null
    val calleeReference = receiver.calleeReference as? FirResolvedNamedReference ?: return null
    return calleeReference.resolvedSymbol as? FirValueParameterSymbol
}

fun FirCallableDeclaration.contextParametersForFunctionOrContainingProperty(): List<FirValueParameter> =
    if (this is FirPropertyAccessor)
        this.propertySymbol.fir.contextParameters
    else
        this.contextParameters

/**
 * A delegated property is allowed to have accessors as long as they don't have a body.
 *
 * Returns `true` when the property is delegated and no accessor was defined in source or the accessor didn't have a body.
 *
 * The function assumes that the body is not lazy.
 */
fun FirPropertyAccessor.hasGeneratedDelegateBody(): Boolean {
    return body?.statements?.firstOrNull()?.source?.kind is KtFakeSourceElementKind.DelegatedPropertyAccessor
}

/**
 * A [FirReplExpressionReference] ([FirProperty.initializer]/[FirProperty.delegate]) of moved property inside a REPL snippet declaration
 */
val FirProperty.replExpressionReference: FirReplExpressionReference?
    get() = when {
        symbol !is FirRegularPropertySymbol || isReplSnippetDeclaration != true -> null
        else -> initializer as? FirReplExpressionReference ?: delegate as? FirReplExpressionReference
    }
