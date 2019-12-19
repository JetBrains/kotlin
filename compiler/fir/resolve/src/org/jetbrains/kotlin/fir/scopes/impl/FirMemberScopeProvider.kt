/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.resolve.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.memberScopeProvider
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class FirMemberScopeProvider : FirSessionComponent {

    private val declaredMemberCache = mutableMapOf<FirClass<*>, FirScope>()
    private val nestedClassifierCache = mutableMapOf<FirClass<*>, FirNestedClassifierScope>()
    private val packageMemberCache = mutableMapOf<FqName, FirPackageMemberScope>()

    fun declaredMemberScope(
        klass: FirClass<*>,
        useLazyNestedClassifierScope: Boolean,
        existingNames: List<Name>?,
        symbolProvider: FirSymbolProvider?
    ): FirScope {
        return declaredMemberCache.getOrPut(klass) {
            FirClassDeclaredMemberScope(klass, useLazyNestedClassifierScope, existingNames, symbolProvider)
        }
    }

    fun nestedClassifierScope(klass: FirClass<*>): FirNestedClassifierScope {
        return nestedClassifierCache.getOrPut(klass) {
            FirNestedClassifierScope(klass)
        }
    }

    // TODO: it's better to cache this scope in ScopeSession
    fun packageMemberScope(fqName: FqName, session: FirSession): FirPackageMemberScope {
        return packageMemberCache.getOrPut(fqName) {
            FirPackageMemberScope(fqName, session)
        }
    }
}

fun declaredMemberScope(klass: FirClass<*>): FirScope {
    return klass
        .session
        .memberScopeProvider
        .declaredMemberScope(klass, useLazyNestedClassifierScope = false, existingNames = null, symbolProvider = null)
}

fun declaredMemberScopeWithLazyNestedScope(
    klass: FirClass<*>,
    existingNames: List<Name>,
    symbolProvider: FirSymbolProvider
): FirScope {
    return klass
        .session
        .memberScopeProvider
        .declaredMemberScope(klass, useLazyNestedClassifierScope = true, existingNames = existingNames, symbolProvider = symbolProvider)
}

fun nestedClassifierScope(klass: FirClass<*>): FirNestedClassifierScope {
    return klass
        .session
        .memberScopeProvider
        .nestedClassifierScope(klass)
}

fun lazyNestedClassifierScope(
    classId: ClassId,
    existingNames: List<Name>,
    symbolProvider: FirSymbolProvider
): FirLazyNestedClassifierScope {
    return FirLazyNestedClassifierScope(classId, existingNames, symbolProvider)
}

fun packageMemberScope(fqName: FqName, session: FirSession): FirPackageMemberScope {
    return session.memberScopeProvider.packageMemberScope(fqName, session)
}

