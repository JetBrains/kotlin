/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import kotlin.reflect.KClass

interface FirSession {


    val components: Map<KClass<*>, Any>

    fun <T : Any> getService(kclass: KClass<T>): T =
        components[kclass] as T
}

inline fun <reified T : Any> FirSession.service(): T =
    getService(T::class)