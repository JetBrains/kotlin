package org.jetbrains.konan.analyser

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.roots.libraries.PersistentLibraryKind
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.util.io.exists
import org.jetbrains.konan.analyser.index.KonanDescriptorManager
import org.jetbrains.konan.settings.KonanPaths
import org.jetbrains.kotlin.analyzer.ResolverForModuleFactory
import org.jetbrains.kotlin.backend.konan.KonanBuiltIns
import org.jetbrains.kotlin.backend.konan.KonanPlatform
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.caches.resolve.IdePlatformSupport
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.context.GlobalContextImpl
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.idea.caches.resolve.PlatformAnalysisSettings
import org.jetbrains.kotlin.resolve.TargetPlatform

/**
 * @author Alefas
 */
class KonanPlatformSupport : IdePlatformSupport() {

  override val resolverForModuleFactory: ResolverForModuleFactory
    get() = KonanAnalyzerFacade()

  override val libraryKind: PersistentLibraryKind<*>?
    get() = null

  override val platform: TargetPlatform
    get() = KonanPlatform

  override fun createBuiltIns(settings: PlatformAnalysisSettings, sdkContext: GlobalContextImpl): KotlinBuiltIns {
    val builtIns = KonanBuiltIns(sdkContext.storageManager)

    //todo: it depends on a random project's stdlib, propagate the actual project here
    val stdlibPath = ProjectManager.getInstance().openProjects.asSequence().mapNotNull {
      val stdlibPath = KonanPaths.getInstance(it).konanStdlib()
      if (stdlibPath != null && stdlibPath.exists()) stdlibPath else null
    }.firstOrNull()

    if (stdlibPath != null) {
      val stdlib = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(stdlibPath.toFile())
      if (stdlib != null) {
        val builtInsModule: ModuleDescriptorImpl = KonanDescriptorManager.getInstance().getCachedLibraryDescriptor(stdlib, LanguageVersionSettingsImpl.DEFAULT)
        builtInsModule.setField("dependencies") { null }
        builtInsModule.setDependencies(builtInsModule)
        builtIns.builtInsModule = builtInsModule
      }
    }

    return builtIns
  }

  override fun isModuleForPlatform(module: Module): Boolean {
    return true
  }
}
