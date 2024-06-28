/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.metadata.impl

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ModuleCapability
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.library.metadata.KlibModuleDescriptorFactory
import org.jetbrains.kotlin.library.metadata.KlibModuleOrigin
import org.jetbrains.kotlin.library.metadata.isCInteropLibrary
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.resolve.ImplicitIntegerCoercion
import org.jetbrains.kotlin.storage.StorageManager

internal class KlibModuleDescriptorFactoryImpl(val createBuiltIns: (StorageManager) -> KotlinBuiltIns) : KlibModuleDescriptorFactory {

    override fun createDescriptor(
        name: Name,
        storageManager: StorageManager,
        builtIns: KotlinBuiltIns,
        origin: KlibModuleOrigin,
        customCapabilities: Map<ModuleCapability<*>, Any?>
    ) = ModuleDescriptorImpl(
        name,
        storageManager,
        builtIns,
        capabilities = customCapabilities + mapOf(
            KlibModuleOrigin.CAPABILITY to origin,
            ImplicitIntegerCoercion.MODULE_CAPABILITY to origin.isCInteropLibrary()
        ),
        // TODO: don't use hardcoded platform; it should be supplied as a parameter
        platform = NativePlatforms.unspecifiedNativePlatform
    )

    override fun createDescriptorAndNewBuiltIns(
        name: Name,
        storageManager: StorageManager,
        origin: KlibModuleOrigin,
        customCapabilities: Map<ModuleCapability<*>, Any?>
    ): ModuleDescriptorImpl {

        val builtIns = createBuiltIns(storageManager)

        val moduleDescriptor = createDescriptor(name, storageManager, builtIns, origin, customCapabilities)
        builtIns.builtInsModule = moduleDescriptor

        return moduleDescriptor
    }
}
