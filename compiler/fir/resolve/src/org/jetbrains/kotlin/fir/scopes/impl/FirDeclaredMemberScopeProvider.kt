/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.ThreadSafeMutableState
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.classId
import org.jetbrains.kotlin.fir.resolve.declaredMemberScopeProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.getOrPutNullable

@ThreadSafeMutableState
class FirDeclaredMemberScopeProvider : FirSessionComponent {
    private val declaredMemberCache = mutableMapOf<FirClass<*>, FirClassDeclaredMemberScope>()
    private val nestedClassifierCache = mutableMapOf<FirClass<*>, FirNestedClassifierScope?>()

    fun getClassByClassId(classId: ClassId): FirClass<*>? {
        for ((clazz, _) in declaredMemberCache) {
            if (clazz.classId.packageFqName == classId.packageFqName
                && clazz.classId.relativeClassName == classId.relativeClassName
            ) {
                return clazz
            }
        }
        for ((clazz, _) in nestedClassifierCache) {
            if (clazz.classId.packageFqName == classId.packageFqName
                && clazz.classId.relativeClassName == classId.relativeClassName
            ) {
                return clazz
            }
        }
        return null
    }

    fun declaredMemberScope(
        klass: FirClass<*>,
        useLazyNestedClassifierScope: Boolean,
        existingNames: List<Name>?,
        symbolProvider: FirSymbolProvider?
    ): FirClassDeclaredMemberScope {
        return declaredMemberCache.getOrPut(klass) {
            FirClassDeclaredMemberScope(klass, useLazyNestedClassifierScope, existingNames, symbolProvider)
        }
    }

    fun nestedClassifierScope(klass: FirClass<*>): FirNestedClassifierScope? {
        return nestedClassifierCache.getOrPutNullable(klass) {
            FirNestedClassifierScope(klass).takeUnless { it.isEmpty() }
        }
    }
}

fun declaredMemberScope(klass: FirClass<*>): FirClassDeclaredMemberScope {
    return klass
        .session
        .declaredMemberScopeProvider
        .declaredMemberScope(klass, useLazyNestedClassifierScope = false, existingNames = null, symbolProvider = null)
}

fun declaredMemberScopeWithLazyNestedScope(
    klass: FirClass<*>,
    existingNames: List<Name>,
    symbolProvider: FirSymbolProvider
): FirScope {
    return klass
        .session
        .declaredMemberScopeProvider
        .declaredMemberScope(klass, useLazyNestedClassifierScope = true, existingNames = existingNames, symbolProvider = symbolProvider)
}

fun nestedClassifierScope(klass: FirClass<*>): FirNestedClassifierScope? {
    return klass
        .session
        .declaredMemberScopeProvider
        .nestedClassifierScope(klass)
}

fun lazyNestedClassifierScope(
    classId: ClassId,
    existingNames: List<Name>,
    symbolProvider: FirSymbolProvider
): FirLazyNestedClassifierScope? {
    if (existingNames.isEmpty()) return null
    return FirLazyNestedClassifierScope(classId, existingNames, symbolProvider)
}
