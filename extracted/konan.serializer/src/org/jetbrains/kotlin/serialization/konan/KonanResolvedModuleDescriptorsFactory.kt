package org.jetbrains.kotlin.serialization.konan

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.konan.library.resolver.KonanLibraryResolveResult
import org.jetbrains.kotlin.storage.StorageManager

interface KonanResolvedModuleDescriptorsFactory {

    val moduleDescriptorFactory: KonanDeserializedModuleDescriptorFactory

    fun createResolved(
            resolvedLibraries: KonanLibraryResolveResult,
            storageManager: StorageManager,
            builtIns: KotlinBuiltIns?,
            languageVersionSettings: LanguageVersionSettings,
            customAction: ((KonanLibrary, ModuleDescriptorImpl) -> Unit)? = null
    ): KonanResolvedModuleDescriptors
}

class KonanResolvedModuleDescriptors(

        val resolvedDescriptors: List<ModuleDescriptorImpl>,

        /**
         * This is a module which "contains" forward declarations.
         * Note: this module should be unique per compilation and should always be the last dependency of any module.
         */
        val forwardDeclarationsModule: ModuleDescriptorImpl
)
