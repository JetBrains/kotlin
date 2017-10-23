/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.konan.descriptors

import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.KonanBuiltIns
import org.jetbrains.kotlin.backend.konan.library.KonanLibraryReader
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.storage.StorageManager

sealed class KonanModuleOrigin {
    companion object {
        val CAPABILITY = ModuleDescriptor.Capability<KonanModuleOrigin>("KonanModuleOrigin")
    }
}

// Note: merging these two concepts (LlvmSymbolOrigin and KonanModuleOrigin) for simplicity:
sealed class LlvmSymbolOrigin : KonanModuleOrigin()

data class DeserializedKonanModule(val reader: KonanLibraryReader) : LlvmSymbolOrigin()
object CurrentKonanModule : LlvmSymbolOrigin()

object SyntheticModules : KonanModuleOrigin()

val ModuleDescriptor.origin get() = this.getCapability(KonanModuleOrigin.CAPABILITY)!!

internal val Context.stdlibModule get() = this.builtIns.any.module

internal fun createKonanModuleDescriptor(
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

internal fun createKonanModuleDescriptor(
        name: Name,
        storageManager: StorageManager,
        builtIns: KotlinBuiltIns,
        origin: KonanModuleOrigin
) = ModuleDescriptorImpl(
        name, storageManager, builtIns,
        capabilities = mapOf(KonanModuleOrigin.CAPABILITY to origin)
)
