/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.scopes.processClassifiersByName

fun FirClass.generatedNestedClassifiers(session: FirSession): List<FirDeclaration> {
    val scope = session.declaredMemberScope(this)
    val result = mutableListOf<FirDeclaration>()
    for (name in scope.getClassifierNames()) {
        scope.processClassifiersByName(name) {
            if (it.fir.origin.generated) {
                result += it.fir
            }
        }
    }
    return result
}

fun FirClass.generatedMembers(session: FirSession): List<FirDeclaration> {
    val scope = session.declaredMemberScope(this)
    val result = mutableListOf<FirDeclaration>()
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
