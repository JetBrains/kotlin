/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.descriptors.ModuleCapability
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.library.metadata.CurrentKlibModuleOrigin
import org.jetbrains.kotlin.library.metadata.KlibModuleOrigin
import org.jetbrains.kotlin.library.metadata.klibModuleOrigin
import org.jetbrains.kotlin.library.metadata.klibModuleOriginOrNull
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * KT-62984: under the K2 frontend, `FirModuleDescriptor.getCapability` returns null for every capability,
 * so a module descriptor without [KlibModuleOrigin] must be survivable for nullable callers
 * (e.g. `ModuleDescriptor.konanLibrary`, which treats a null origin as "part of the current module").
 */
class KlibModuleOriginContractTest {
    @Test
    fun testModuleWithoutCapabilityHasNullOrigin() {
        assertNull(moduleDescriptor(capabilities = emptyMap()).klibModuleOriginOrNull)
    }

    @Test
    fun testModuleWithoutCapabilityFailsWithNamedError() {
        val module = moduleDescriptor(capabilities = emptyMap())
        val exception = assertThrows<IllegalStateException> { module.klibModuleOrigin }
        assertTrue(exception.message!!.contains(MODULE_NAME)) {
            "Error message should name the module, got: ${exception.message}"
        }
    }

    @Test
    fun testModuleWithCapabilityKeepsItsOrigin() {
        val module = moduleDescriptor(capabilities = mapOf(KlibModuleOrigin.CAPABILITY to CurrentKlibModuleOrigin))
        assertEquals(CurrentKlibModuleOrigin, module.klibModuleOriginOrNull)
        assertEquals(CurrentKlibModuleOrigin, module.klibModuleOrigin)
    }

    private fun moduleDescriptor(capabilities: Map<ModuleCapability<*>, Any?>): ModuleDescriptorImpl =
        ModuleDescriptorImpl(
            Name.special(MODULE_NAME),
            LockBasedStorageManager.NO_LOCKS,
            DefaultBuiltIns.Instance,
            capabilities = capabilities,
        )

    companion object {
        private const val MODULE_NAME = "<moduleWithoutKlibOrigin>"
    }
}
