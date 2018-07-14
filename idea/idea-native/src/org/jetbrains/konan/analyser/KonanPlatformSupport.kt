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
    val storageManager = sdkContext.storageManager
    val builtIns = KonanBuiltIns(storageManager)

    //todo: it depends on a random project's stdlib, propagate the actual project here
    val konanStdlib = ProjectManager.getInstance().openProjects.asSequence().mapNotNull { KonanPaths.getInstance(it).konanStdlib() }.firstOrNull()
    if (konanStdlib != null && konanStdlib.exists()) {
      val stdLib = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(konanStdlib.toFile())
      if (stdLib != null) {
        val manager = KonanDescriptorManager.INSTANCE
        val builtInsModule: ModuleDescriptorImpl = manager.getDescriptor(stdLib, LanguageVersionSettingsImpl.DEFAULT)
        builtInsModule.setField("dependencies", { null })
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