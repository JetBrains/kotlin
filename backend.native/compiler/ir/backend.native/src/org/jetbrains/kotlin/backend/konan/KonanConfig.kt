package org.jetbrains.kotlin.backend.konan

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.backend.konan.KonanPlatform
import org.jetbrains.kotlin.backend.konan.llvm.loadMetadata
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

class KonanConfig(val project: Project, val configuration: CompilerConfiguration) {

    private val libraries = configuration.getList(KonanConfigKeys.LIBRARY_FILES)

    private val loadedDescriptors = loadLibMetadata(libraries)

    val moduleId: String
        get() = configuration.getNotNull(CommonConfigurationKeys.MODULE_NAME)

    fun loadLibMetadata(libraries: List<String>): List<ModuleDescriptorImpl> {

        val allMetadata = mutableListOf<ModuleDescriptorImpl>()

        for (path in libraries) {
            val filePath = File(path)
            if (!filePath.exists()) {
                error("Path '" + path + "' does not exist");
            }

            val moduleDescriptor = loadMetadata(configuration, filePath);

            allMetadata.add(moduleDescriptor);
        }
        return allMetadata;
    }

    internal val moduleDescriptors: List<ModuleDescriptorImpl> by lazy {
        for (module in loadedDescriptors) {
            // Yes, just to all of them.
            setDependencies(module, loadedDescriptors)
        }

        loadedDescriptors 
    }

    companion object {
        private fun setDependencies(module: ModuleDescriptorImpl, modules: List<ModuleDescriptorImpl>) {
            module.setDependencies(modules.plus(KonanPlatform.builtIns.builtInsModule))
        }
    }

}
