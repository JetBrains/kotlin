/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.utils.Jsr305State
import kotlin.reflect.KClass

abstract class FirSession(val sessionProvider: FirSessionProvider?) {
    open val moduleInfo: ModuleInfo? get() = null

    val jsr305State: Jsr305State? get() = null


    val components: MutableMap<KClass<*>, Any> = mutableMapOf()

    var _firSymbolProvider: Any? = null


    fun <T : Any> getService(kclass: KClass<T>): T =
        components[kclass] as T

    protected fun <T : Any> registerComponent(tClass: KClass<T>, t: T) {
        assert(tClass !in components) { "Already registered component" }
        components[tClass] = t
    }
}

interface FirSessionProvider {
    fun getSession(moduleInfo: ModuleInfo): FirSession?
}

inline fun <reified T : Any> FirSession.service(): T =
    getService(T::class)