/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.util.AttributeArrayOwner
import org.jetbrains.kotlin.util.TypeRegistry
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

abstract class FirModuleCapability<T: FirModuleCapability<T>> {
    abstract val key: KClass<out T>
}

class FirModuleCapabilities private constructor(capabilities: List<FirModuleCapability<*>>): AttributeArrayOwner<FirModuleCapability<*>, FirModuleCapability<*>>() {

    companion object : TypeRegistry<FirModuleCapability<*>, FirModuleCapability<*>>() {

        val Empty: FirModuleCapabilities = FirModuleCapabilities(emptyList())

        fun create(attributes: List<FirModuleCapability<*>>): FirModuleCapabilities {
            return if (attributes.isEmpty()) {
                Empty
            } else {
                FirModuleCapabilities(attributes)
            }
        }
        override fun ConcurrentHashMap<String, Int>.customComputeIfAbsent(key: String, compute: (String) -> Int): Int {
            return this[key] ?: synchronized(this) {
                this[key] ?: compute(key).also { this.putIfAbsent(key, it) }
            }
        }
    }

    init {
        for (capability in capabilities) {
            registerComponent(capability.key, capability)
        }
    }


    override val typeRegistry: TypeRegistry<FirModuleCapability<*>, FirModuleCapability<*>>
        get() = Companion
}