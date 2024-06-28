/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.util.ConeTypeRegistry
import org.jetbrains.kotlin.util.AttributeArrayOwner
import org.jetbrains.kotlin.util.TypeRegistry
import kotlin.reflect.KClass

abstract class FirModuleCapability {
    abstract val key: KClass<out FirModuleCapability>
}

class FirModuleCapabilities private constructor(
    capabilities: List<FirModuleCapability>
) : AttributeArrayOwner<FirModuleCapability, FirModuleCapability>() {

    companion object : ConeTypeRegistry<FirModuleCapability, FirModuleCapability>() {

        val Empty: FirModuleCapabilities = FirModuleCapabilities(emptyList())

        fun create(attributes: List<FirModuleCapability>): FirModuleCapabilities {
            return if (attributes.isEmpty()) {
                Empty
            } else {
                FirModuleCapabilities(attributes)
            }
        }
    }

    init {
        for (capability in capabilities) {
            registerComponent(capability.key, capability)
        }
    }


    override val typeRegistry: TypeRegistry<FirModuleCapability, FirModuleCapability>
        get() = Companion
}
