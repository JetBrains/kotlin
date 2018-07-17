package org.jetbrains.konan.analyser

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
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
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.resolve.TargetEnvironment
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.storage.StorageManager
import java.nio.file.Path

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
    val declarationProviderFactory = createDeclarationProviderFactory(project, moduleContext, syntheticFiles, moduleContent.moduleInfo,
                                                                      moduleContentScope)

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
    val libraryPaths = konanPaths.libraryPaths().toMutableList()
    libraryPaths.addAll(konanPaths.konanPlatformLibraries())

    val module = resolveModule(syntheticFiles, project)

    fun setLibraryForDescriptor(path: Path, descriptor: ModuleDescriptorImpl) {
      val library = findLibrary(module, path) ?: return
      //todo: replace reflection by changes to kotlin-native?
      descriptor.setField("capabilities", { oldValue ->
        val capabilities = oldValue as Map<*, *>
        val libraryInfo = createLibraryInfo(project, library)
        capabilities + Pair(ModuleInfo.Capability, libraryInfo)
      })
    }

    fun createDescriptor(path: Path): ModuleDescriptorImpl? {
      val file = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(path.toFile())
      return if (file != null && file.exists()) {
        val descriptor = KonanDescriptorManager.INSTANCE.getDescriptor(file, languageVersionSettings)
        setLibraryForDescriptor(path, descriptor)
        descriptor
      }
      else null
    }

    val descriptors = libraryPaths.mapNotNull(::createDescriptor)
    val stdlibDescriptor = konanPaths.konanStdlib()?.let(::createDescriptor)

    val dependencies = descriptors.toMutableList()
    if (stdlibDescriptor != null)
      dependencies.add(stdlibDescriptor)

    // Create module for handling `cnames.structs` opaque declarations. It should be singleton and last dependency in the list.
    val builtIns = moduleContext.module.builtIns
    val storageManager: StorageManager = moduleContext.storageManager
    val forwardDeclarationsModule = createForwardDeclarationsModule(builtIns, storageManager)

    for (descriptor in descriptors) {
      descriptor.setField("dependencies", { null })
      descriptor.setDependencies(descriptor, stdlibDescriptor!!, forwardDeclarationsModule)
    }

    stdlibDescriptor?.let {
      it.setField("dependencies", { null })
      it.setDependencies(listOf(it))
    }

    val fragmentProviders = mutableListOf(packageFragmentProvider)
    descriptors.mapTo(fragmentProviders) { it.packageFragmentProvider }
    fragmentProviders.add(forwardDeclarationsModule.packageFragmentProvider)

    return ResolverForModule(CompositePackageFragmentProvider(fragmentProviders), container)
  }

  private fun resolveModule(syntheticFiles: MutableCollection<KtFile>,
                            project: Project): Module {
    val virtualFilePath = try {
      syntheticFiles.firstOrNull()?.virtualFilePath
    }
    catch (e: IllegalStateException) {
      null
    }
    return virtualFilePath
             ?.run { VfsUtil.findFileByIoFile(java.io.File(this), false) }
             ?.run { ProjectFileIndex.SERVICE.getInstance(project).getModuleForFile(this) }
           ?: ModuleManager.getInstance(project).modules.first()
  }

  private fun findLibrary(module: Module, path: Path): Library? {
    var lib: Library? = null
    ModuleRootManager.getInstance(module).orderEntries().forEachLibrary {
      if (it.name?.substringAfter(": ") == path.fileName.toString()) {
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
