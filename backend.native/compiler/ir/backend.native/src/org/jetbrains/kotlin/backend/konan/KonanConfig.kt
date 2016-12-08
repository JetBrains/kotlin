package org.jetbrains.kotlin.backend.konan

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.backend.konan.KonanPlatform
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.CompilerDeserializationConfiguration
import org.jetbrains.kotlin.serialization.js.JsModuleDescriptor
import org.jetbrains.kotlin.serialization.js.KotlinJavascriptSerializationUtil
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.storage.LockBasedStorageManager

import org.jetbrains.kotlin.backend.konan.llvm.KotlinKonanMetadata
import org.jetbrains.kotlin.backend.konan.llvm.KotlinKonanMetadataUtils

class KonanConfig(val project: Project, val configuration: CompilerConfiguration) {

    private val storageManager = LockBasedStorageManager()

    private val libraries = configuration.getList(KonanConfigKeys.LIBRARY_FILES)

    private val metadata = KotlinKonanMetadataUtils.loadLibMetadata(libraries)

    val moduleId: String
        get() = configuration.getNotNull(CommonConfigurationKeys.MODULE_NAME)

    val moduleKind: ModuleKind
        get() = configuration.get(KonanConfigKeys.MODULE_KIND)!!

    // We reuse JsModuleDescriptor for serialization for now, as we haven't got one for Konan yet.
    internal val moduleDescriptors: MutableList<JsModuleDescriptor<ModuleDescriptorImpl>> by lazy {

        val jsDescriptors = mutableListOf<JsModuleDescriptor<ModuleDescriptorImpl>>()
        val descriptors = mutableListOf<ModuleDescriptorImpl>()

        for (metadataEntry in metadata) {
            val descriptor = createModuleDescriptor(metadataEntry)
            jsDescriptors.add(descriptor)
            descriptors.add(descriptor.data)
        }

        for (module in jsDescriptors) {
            setDependencies(module.data, descriptors)
        }

        jsDescriptors 
    }

    private fun createModuleDescriptor(metadata: KotlinKonanMetadata): JsModuleDescriptor<ModuleDescriptorImpl> {

        val moduleDescriptor = ModuleDescriptorImpl(
                Name.special("<" + metadata.moduleName + ">"), storageManager, KonanPlatform.builtIns)

        val rawDescriptor = KotlinJavascriptSerializationUtil.readModule(
                metadata.body, storageManager, moduleDescriptor, CompilerDeserializationConfiguration(
                configuration.get(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS, LanguageVersionSettingsImpl.DEFAULT)))

        val provider = rawDescriptor.data
        moduleDescriptor.initialize(provider ?: PackageFragmentProvider.Empty)

        return rawDescriptor.copy(moduleDescriptor)
    }

    companion object {
        private fun setDependencies(module: ModuleDescriptorImpl, modules: List<ModuleDescriptorImpl>) {
            module.setDependencies(modules.plus(KonanPlatform.builtIns.builtInsModule))
        }
    }

}
