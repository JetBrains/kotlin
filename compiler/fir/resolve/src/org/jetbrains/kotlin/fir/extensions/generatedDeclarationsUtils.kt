/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.scopes.processClassifiersByName
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol

fun FirRegularClass.generatedNestedClassifiers(session: FirSession): List<FirClassLikeDeclaration> {
    val scope = session.declaredMemberScope(this, memberRequiredPhase = null)
    val result = mutableListOf<FirClassLikeDeclaration>()
    for (name in scope.getClassifierNames()) {
        scope.processClassifiersByName(name) {
            if (it.fir.origin.generated) {
                // Plugins can not generate type parameters, so it's safe to cast symbol here
                require(it is FirClassLikeSymbol<*>) { "Plugins can not generate type parameters, but had $it" }
                result += it.fir
            }
        }
    }
    return result
}

fun FirRegularClass.generatedMembers(session: FirSession): List<FirCallableDeclaration> {
    val scope = session.declaredMemberScope(this, memberRequiredPhase = null)
    val result = mutableListOf<FirCallableDeclaration>()
    for (name in scope.getCallableNames()) {
        scope.processFunctionsByName(name) {
            if (it.fir.origin.generated) {
                result += it.fir
            }
        }
        scope.processPropertiesByName(name) {
            if (it.fir.origin.generated) {
                result += it.fir
            }
        }
    }
    scope.processDeclaredConstructors {
        if (it.fir.origin.generated) {
            result += it.fir
        }
    }
    return result
}
