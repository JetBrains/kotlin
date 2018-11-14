/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import kotlin.reflect.KClass

abstract class FirSessionBase : FirSession {

    protected fun <T : Any> registerComponent(tClass: KClass<T>, t: T) {
        assert(tClass !in components) { "Already registered component" }
        components[tClass] = t
    }

    override val components: MutableMap<KClass<*>, Any> = mutableMapOf()

}