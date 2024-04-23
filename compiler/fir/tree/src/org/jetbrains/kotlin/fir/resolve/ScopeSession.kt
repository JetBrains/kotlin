/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.util.PrivateForInline

class ScopeSession {
    private val scopes = hashMapOf<Any, HashMap<ScopeSessionKey<*, *>, Any>>()

    @PrivateForInline
    fun scopes() = scopes

    @OptIn(PrivateForInline::class)
    inline fun <reified ID : Any, reified FS : Any> getOrBuild(id: ID, key: ScopeSessionKey<ID, FS>, build: () -> FS): FS {
        return scopes().getOrPut(id) {
            hashMapOf()
        }.getOrPut(key) {
            build()
        } as FS
    }


    data class CacheKey(
        val clazz: FirClass,
        val withForcedTypeCalculator: Boolean,
        val memberRequiredPhase: FirResolvePhase?,
    )

    val unsubstitutedScopeCache = hashMapOf<CacheKey, FirTypeScope> ()
}

abstract class ScopeSessionKey<ID : Any, FS : Any>

