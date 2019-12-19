/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.scopes.FirScope

class ScopeSession {
    private val scopes = hashMapOf<Any, HashMap<ScopeSessionKey<*, *>, FirScope>>()
    fun <ID : Any, FS : FirScope> getOrBuild(id: ID, key: ScopeSessionKey<ID, FS>, build: () -> FS): FS {
        return scopes.getOrPut(id) {
            hashMapOf()
        }.getOrPut(key) {
            build()
        } as FS
    }
}

abstract class ScopeSessionKey<ID : Any, FS : FirScope>

