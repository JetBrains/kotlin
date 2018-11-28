/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.konan

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.PersistentLibraryKind
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.PathUtil
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.caches.resolve.IdePlatformKindResolution
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider
import org.jetbrains.kotlin.descriptors.konan.DeserializedKonanModuleOrigin
import org.jetbrains.kotlin.ide.konan.analyzer.NativeAnalyzerFacade
import org.jetbrains.kotlin.idea.caches.project.LibraryInfo
import org.jetbrains.kotlin.idea.caches.project.getModuleInfosFromIdeaModel
import org.jetbrains.kotlin.idea.caches.resolve.PlatformAnalysisSettings
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.*
import org.jetbrains.kotlin.konan.util.KonanFactories
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.impl.NativeIdePlatformKind
import org.jetbrains.kotlin.resolve.CompilerDeserializationConfiguration
import org.jetbrains.kotlin.resolve.ImplicitIntegerCoercion
import org.jetbrains.kotlin.resolve.konan.platform.KonanPlatform

class NativePlatformKindResolution : IdePlatformKindResolution {

    override fun createLibraryInfo(project: Project, library: Library): List<LibraryInfo> {
        return library.getFiles(OrderRootType.CLASSES)
            .mapNotNull { file -> PathUtil.getLocalPath(file) }
            .map { path -> File(path) }
            .map { file -> NativeLibraryInfo(project, library, file) }
    }

    override fun isLibraryFileForPlatform(virtualFile: VirtualFile): Boolean {
        return when {
            // The virtual file for a library packed in a ZIP file will have path like "/some/path/to/the/file.klib!/",
            // and therefore will be recognized by VFS as a directory (isDirectory == true).
            // So, first, let's check the extension.
            virtualFile.extension == KLIB_FILE_EXTENSION -> true

            virtualFile.isDirectory -> {
                val linkdataDir = virtualFile.findChild("linkdata") ?: return false
                // False means we hit .knm file
                !VfsUtil.processFilesRecursively(linkdataDir) {
                    it.extension != KLIB_METADATA_FILE_EXTENSION
                }
            }

            else -> false
        }
    }

    override val libraryKind: PersistentLibraryKind<*>?
        get() = NativeLibraryKind

    override val kind get() = NativeIdePlatformKind

    override val resolverForModuleFactory get() = NativeAnalyzerFacade

    override fun createBuiltIns(settings: PlatformAnalysisSettings, projectContext: ProjectContext) =
        createKotlinNativeBuiltIns(projectContext)
}

private fun createKotlinNativeBuiltIns(projectContext: ProjectContext): KotlinBuiltIns {

    // TODO: It depends on a random project's stdlib, propagate the actual project here.
    fun findStdlib(): Pair<String, LibraryInfo>? {
        getModuleInfosFromIdeaModel(projectContext.project, KonanPlatform).forEach { module ->
            module.dependencies().forEach { dependency ->
                (dependency as? LibraryInfo)?.getLibraryRoots()?.forEach { path ->
                    if (path.endsWith(KONAN_STDLIB_NAME)) {
                        return path to dependency
                    }
                }
            }
        }
        return null
    }

    val stdlib: Pair<String, LibraryInfo>? = findStdlib()

    if (stdlib != null) {

        val (path, libraryInfo) = stdlib
        val library = createKonanLibrary(
            File(path),
            KOTLIN_NATIVE_CURRENT_ABI_VERSION,
            metadataReader = CachingIdeMetadataReaderImpl
        )

        val libraryProto = library.moduleHeaderData

        val moduleName = Name.special(libraryProto.moduleName)
        val moduleOrigin = DeserializedKonanModuleOrigin(library)

        val storageManager = projectContext.storageManager
        val descriptorFactory = KonanFactories.DefaultDescriptorFactory

        val builtInsModule = descriptorFactory.createDescriptorAndNewBuiltIns(
            moduleName,
            storageManager,
            moduleOrigin,
            libraryInfo.capabilities
        )

        val deserializationConfiguration = CompilerDeserializationConfiguration(LanguageVersionSettingsImpl.DEFAULT)

        val provider = KonanFactories.DefaultPackageFragmentsFactory.createPackageFragmentProvider(
            library,
            null,
            libraryProto.packageFragmentNameList,
            storageManager,
            builtInsModule,
            deserializationConfiguration
        )


        builtInsModule.initialize(
            CompositePackageFragmentProvider(
                listOf(
                    provider,
                    KonanFactories.DefaultPackageFragmentsFactory.createForwardDeclarationHackPackagePartProvider(
                        storageManager,
                        builtInsModule
                    )
                )
            )
        )

        builtInsModule.setDependencies(listOf(builtInsModule))

        return builtInsModule.builtIns
    }

    return DefaultBuiltIns.Instance
}

class NativeLibraryInfo(project: Project, library: Library, private val root: File) : LibraryInfo(project, library) {

    private val nativeLibrary = createKonanLibrary(
        root,
        KOTLIN_NATIVE_CURRENT_ABI_VERSION,
        metadataReader = CachingIdeMetadataReaderImpl
    )

    override fun getLibraryRoots(): Collection<String> {
        return listOf(root.absolutePath)
    }

    override val capabilities: Map<ModuleDescriptor.Capability<*>, Any?>
        get() = super.capabilities +
                mapOf(
                    ImplicitIntegerCoercion.MODULE_CAPABILITY to nativeLibrary.isInterop,
                    NATIVE_LIBRARY_CAPABILITY to nativeLibrary
                )

    companion object {
        val NATIVE_LIBRARY_CAPABILITY = ModuleDescriptor.Capability<KonanLibrary>("KonanLibrary")
    }
}