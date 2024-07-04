/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.getContainingDeclaration
import org.jetbrains.kotlin.fir.resolve.outerType
import org.jetbrains.kotlin.fir.resolve.scope
import org.jetbrains.kotlin.fir.scopes.CallableCopyTypeCalculator
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.impl.TypeAliasConstructorsSubstitutingScope
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol

fun FirClassLikeSymbol<*>.expandedClassWithConstructorsScope(
    session: FirSession,
    scopeSession: ScopeSession,
    memberRequiredPhaseForRegularClasses: FirResolvePhase?,
): Pair<FirRegularClassSymbol, FirScope>? {
    return when (this) {
        is FirRegularClassSymbol -> this to unsubstitutedScope(
            session, scopeSession,
            withForcedTypeCalculator = false,
            memberRequiredPhase = memberRequiredPhaseForRegularClasses,
        )
        is FirTypeAliasSymbol -> {
            val expandedType = resolvedExpandedTypeRef.coneType as? ConeClassLikeType ?: return null
            val expandedClass = expandedType.toRegularClassSymbol(session) ?: return null
            val expandedTypeScope = expandedType.scope(
                session, scopeSession,
                CallableCopyTypeCalculator.DoNothing,
                // Must be `STATUS`; otherwise we can't create substitution overrides for constructor symbols,
                // which we need to map typealias arguments to the expanded type arguments, which happens when
                // we request declared constructor symbols from the scope returned below.
                // See: `LLFirPreresolvedReversedDiagnosticCompilerFE10TestDataTestGenerated.testTypealiasAnnotationWithFixedTypeArgument`
                requiredMembersPhase = FirResolvePhase.STATUS,
            ) ?: return null

            val outerType = outerType(expandedType, session) { it.getContainingDeclaration(session) }
            expandedClass to TypeAliasConstructorsSubstitutingScope(this, expandedTypeScope, outerType, abbreviation = null)
        }
        else -> null
    }
}
