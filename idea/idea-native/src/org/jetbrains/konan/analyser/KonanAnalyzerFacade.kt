package org.jetbrains.konan.analyser

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.vfs.LocalFileSystem
import org.jetbrains.konan.KotlinWorkaroundUtil.*
import org.jetbrains.konan.analyser.index.KonanDescriptorManager
import org.jetbrains.konan.settings.KonanPaths
import org.jetbrains.kotlin.analyzer.*
import org.jetbrains.kotlin.backend.konan.KonanPlatform
import org.jetbrains.kotlin.backend.konan.descriptors.createForwardDeclarationsModule
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.TargetPlatformVersion
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.frontend.di.createContainerForLazyResolve
import org.jetbrains.kotlin.idea.caches.project.ModuleProductionSourceInfo
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.resolve.TargetEnvironment
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.storage.StorageManager
import java.io.File
import java.net.URI

/**
 * @author Alefas
 */

class KonanAnalyzerFacade : ResolverForModuleFactory() {
  override val targetPlatform: TargetPlatform
    get() = KonanPlatform

  override fun <M : ModuleInfo> createResolverForModule(
    moduleDescriptor: ModuleDescriptorImpl,
    moduleContext: ModuleContext,
    moduleContent: ModuleContent<M>,
    platformParameters: PlatformAnalysisParameters,
    targetEnvironment: TargetEnvironment,
    resolverForProject: ResolverForProject<M>,
    languageVersionSettings: LanguageVersionSettings,
    targetPlatformVersion: TargetPlatformVersion): ResolverForModule {

    val (syntheticFiles, moduleContentScope) = destructModuleContent(moduleContent)
    val project = getProject(moduleContext)
    val declarationProviderFactory = createDeclarationProviderFactory(project, moduleContext, syntheticFiles,
                                                                      moduleContent.moduleInfo, moduleContentScope)

    val container = createContainerForLazyResolve(
      moduleContext,
      declarationProviderFactory,
      BindingTraceContext(),
      KonanPlatform,
      TargetPlatformVersion.NoVersion,
      targetEnvironment,
      languageVersionSettings
    )

    val packageFragmentProvider = container.get<ResolveSession>().packageFragmentProvider

    val konanPaths = KonanPaths.getInstance(project)

    val moduleInfo = moduleContent.moduleInfo as? ModuleProductionSourceInfo
    val module = moduleInfo?.let { getModule(it) }

    fun setLibraryForDescriptor(libraryPath: File, descriptor: ModuleDescriptorImpl) {
      if (module == null) return
      val library = findLibrary(module, libraryPath) ?: return
      //todo: replace reflection by changes to kotlin-native?
      descriptor.setField("capabilities") { oldValue ->
        val capabilities = oldValue as Map<*, *>
        val libraryInfo = createLibraryInfo(project, library)
        capabilities + Pair(ModuleInfo.Capability, libraryInfo)
      }
    }

    fun createDescriptor(libraryPath: File): ModuleDescriptorImpl? {
      val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(libraryPath)
      return if (virtualFile != null && virtualFile.exists()) {
        val libraryDescriptor = KonanDescriptorManager.INSTANCE.getDescriptor(virtualFile, languageVersionSettings)
        setLibraryForDescriptor(libraryPath, libraryDescriptor)
        libraryDescriptor
      }
      else null
    }

    val libraryDescriptors = mutableListOf<ModuleDescriptorImpl>()

    if (module != null) {
      ModuleRootManager.getInstance(module).orderEntries().forEachLibrary { library ->

        val libraryPaths = library.getUrls(OrderRootType.CLASSES).map {
          // cut off the schema ("jar:///" for ZIP files or "file:///" for directories)
          // if this is a .klib file, then remove the rightmost part of the path that goes after the name of the file: "!/..."
          File(URI(it).path.replaceAfterLast(".klib", ""))
        }

        libraryPaths.forEach { createDescriptor(it)?.also { libraryDescriptors.add(it) } }

        true // continue the loop while there are more libs
      }
    }

    val stdlibDescriptor = konanPaths.konanStdlib()?.let { createDescriptor(it.toFile()) }

    // Create module for handling `cnames.structs` opaque declarations. It should be singleton and last dependency in the list.
    val storageManager: StorageManager = moduleContext.storageManager
    val builtIns = moduleContext.module.builtIns
    val forwardDeclarationsModule = createForwardDeclarationsModule(builtIns, storageManager)

    if (stdlibDescriptor != null) {
      for (libraryDescriptor in libraryDescriptors) {
        libraryDescriptor.setField("dependencies") { null }
        libraryDescriptor.setDependencies(libraryDescriptor, stdlibDescriptor, forwardDeclarationsModule)
      }

      stdlibDescriptor.setField("dependencies") { null }
      stdlibDescriptor.setDependencies(stdlibDescriptor)
    }

    val fragmentProviders = mutableListOf(packageFragmentProvider)
    libraryDescriptors.mapTo(fragmentProviders) { it.packageFragmentProvider }
    fragmentProviders.add(forwardDeclarationsModule.packageFragmentProvider)
    return ResolverForModule(CompositePackageFragmentProvider(fragmentProviders), container)
  }

  private fun findLibrary(module: Module, libraryPath: File): Library? {
    val libraryName = libraryPath.name
    var lib: Library? = null
    ModuleRootManager.getInstance(module).orderEntries().forEachLibrary {
      if (it.name?.substringAfter(": ") == libraryName) {
        lib = it
        false
      }
      else {
        true
      }
    }
    return lib
  }
}

internal fun Any.setField(fieldName: String, update: (Any?) -> Any?) {
  val field = javaClass.declaredFields.find { it.name == fieldName }
  if (field != null) {
    field.isAccessible = true
    val oldValue = field.get(this)
    val newValue = update(oldValue)
    field.set(this, newValue)
  }
}
