package org.jetbrains.kotlin.descriptors.konan

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.storage.StorageManager

fun createKonanModuleDescriptor(
        name: Name,
        storageManager: StorageManager,
        origin: KonanModuleOrigin
): ModuleDescriptorImpl {
    val builtIns = KonanBuiltIns(storageManager)

    val moduleDescriptor = createKonanModuleDescriptor(name, storageManager, builtIns, origin)

    builtIns.builtInsModule = moduleDescriptor
    // It is mostly correct because any module should depend on stdlib.

    return moduleDescriptor
}

fun createKonanModuleDescriptor(
        name: Name,
        storageManager: StorageManager,
        builtIns: KotlinBuiltIns,
        origin: KonanModuleOrigin
) = ModuleDescriptorImpl(
        name, storageManager, builtIns,
        capabilities = mapOf(KonanModuleOrigin.CAPABILITY to origin)
)

private val STDLIB_MODULE_NAME = Name.special("<stdlib>")

fun ModuleDescriptor.isKonanStdlib() = name == STDLIB_MODULE_NAME
