package org.jetbrains.konan.analyser

import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.vfs.LocalFileSystem
import org.jetbrains.konan.KotlinWorkaroundUtil.*
import org.jetbrains.konan.analyser.index.KonanDescriptorManager
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

    val moduleInfo = moduleContent.moduleInfo as? ModuleProductionSourceInfo
    val module = moduleInfo?.let { getModule(it) }

    fun createLibraryDescriptor(library: Library): ModuleDescriptorImpl? {

      val libraryPath = library.getUrls(OrderRootType.CLASSES).map {
        // cut off the schema ("jar:///" for ZIP files or "file:///" for directories)
        // if this is a .klib file, then remove the rightmost part of the path that goes after the name of the file: "!/..."
        File(URI(it).path.replaceAfterLast(".klib", ""))
      }.first() // first() because a library should have one location

      val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(libraryPath)

      return if (virtualFile != null && virtualFile.exists()) {
        val libraryDescriptor = KonanDescriptorManager.INSTANCE.getDescriptor(virtualFile, languageVersionSettings)

        //todo: replace reflection by changes to kotlin-native?
        libraryDescriptor.setField("capabilities") { oldValue ->
          val capabilities = oldValue as Map<*, *>
          val libraryInfo = createLibraryInfo(project, library)
          capabilities + Pair(ModuleInfo.Capability, libraryInfo)
        }

        libraryDescriptor
      }
      else null
    }

    val libraryDescriptors = mutableListOf<ModuleDescriptorImpl>()
    var stdlibDescriptor: ModuleDescriptorImpl? = null

    if (module != null) {
      ModuleRootManager.getInstance(module).orderEntries().forEachLibrary { library ->
        createLibraryDescriptor(library)?.also {
          libraryDescriptors.add(it)
          if (library.name?.substringAfter(": ") == "stdlib" ) {
            stdlibDescriptor = it
          }
        }
        true // continue the loop while there are more libs
      }
    }

    // Create a module for handling `cnames.structs` opaque declarations. It should be a singleton and the last dependency in the list.
    val forwardDeclarationsDescriptor = createForwardDeclarationsModule(moduleContext.module.builtIns, moduleContext.storageManager)

    for (libraryDescriptor in libraryDescriptors) {
      libraryDescriptor.setField("dependencies") { null }

      val dependencies = mutableListOf(libraryDescriptor)
      if (stdlibDescriptor != null && stdlibDescriptor != libraryDescriptor) {
        // don't add stdlib if it's in fact absent, or if it's the current library on the loop
        dependencies.add(stdlibDescriptor!!)
      }
      dependencies.add(forwardDeclarationsDescriptor)

      libraryDescriptor.setDependencies(dependencies)
    }

    val fragmentProviders = mutableListOf(packageFragmentProvider)
    libraryDescriptors.mapTo(fragmentProviders) { it.packageFragmentProvider }
    fragmentProviders.add(forwardDeclarationsDescriptor.packageFragmentProvider)
    return ResolverForModule(CompositePackageFragmentProvider(fragmentProviders), container)
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
