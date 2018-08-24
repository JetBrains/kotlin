package org.jetbrains.kotlin.descriptors.konan

import org.jetbrains.kotlin.descriptors.ModuleDescriptor

sealed class KonanModuleOrigin {

    companion object {
        val CAPABILITY = ModuleDescriptor.Capability<KonanModuleOrigin>("KonanModuleOrigin")
    }
}

sealed class CompiledKonanModuleOrigin: KonanModuleOrigin()

// FIXME(ddol): replace `Any` by `KonanLibrary` when ready
class DeserializedKonanModuleOrigin(val library: Any) : CompiledKonanModuleOrigin()

object CurrentKonanModuleOrigin: CompiledKonanModuleOrigin()

object SyntheticModulesOrigin : KonanModuleOrigin()

val ModuleDescriptor.konanModuleOrigin get() = this.getCapability(KonanModuleOrigin.CAPABILITY)!!
