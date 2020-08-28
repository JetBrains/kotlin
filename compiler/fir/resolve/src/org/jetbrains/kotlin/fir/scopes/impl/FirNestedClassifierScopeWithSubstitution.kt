/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.isInner
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.transformers.createSubstitutionForSupertype
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.name.Name

class FirNestedClassifierScopeWithSubstitution(
    private val scope: FirNestedClassifierScope,
    private val substitutor: ConeSubstitutor
) : FirScope() {
    override fun processClassifiersByNameWithSubstitution(name: Name, processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit) {
        val matchedClass = scope.getClassifierByName(name) ?: return
        val substitutor = substitutor.takeIf { matchedClass.fir.isInner } ?: ConeSubstitutor.Empty
        processor(matchedClass, substitutor)
    }
}

fun FirScope.wrapNestedClassifierScopeWithSubstitutionForSuperType(
    superType: ConeClassLikeType,
    session: FirSession
): FirScope = if (this is FirNestedClassifierScope) {
    val substitutor = createSubstitutionForSupertype(superType, session)
    FirNestedClassifierScopeWithSubstitution(this, substitutor)
} else {
    this
}