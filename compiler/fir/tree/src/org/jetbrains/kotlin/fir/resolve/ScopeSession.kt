/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.util.PrivateForInline

class ScopeSession {
    private val scopes: HashMap<Any, HashMap<ScopeSessionKey<*, *>, Any>> = hashMapOf()

    @PrivateForInline
    fun scopes(): HashMap<Any, HashMap<ScopeSessionKey<*, *>, Any>> = scopes

    @OptIn(PrivateForInline::class)
    inline fun <reified ID : Any, reified FS : Any> getOrBuild(id: ID, key: ScopeSessionKey<ID, FS>, build: () -> FS): FS {
        return scopes().getOrPut(id) {
            hashMapOf()
        }.getOrPut(key) {
            build()
        } as FS
    }
}

abstract class ScopeSessionKey<ID : Any, FS : Any>

