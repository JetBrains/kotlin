package org.jetbrains.kotlin.library.metadata

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.library.metadata.resolver.KotlinLibraryResolveResult
import org.jetbrains.kotlin.storage.StorageManager
import java.nio.file.Path

interface KlibResolvedModuleDescriptorsFactory {

    val moduleDescriptorFactory: KlibMetadataModuleDescriptorFactory

    /**
     * Given the [resolvedLibraries] creates the list of [ModuleDescriptorImpl]s with properly installed
     * inter-dependencies. The result of this method is returned in a form of [KlibResolvedModuleDescriptors] instance.
     *
     * Please use this method with care: Unless this method accepts `null` for [builtIns], it is not recommended to
     * invoke it this way. If you are compiling a source module, please supply the non-null [builtIns] from the
     * source module, so that all modules created in your compilation session will share the same built-ins instance.
     *
     * Otherwise (if `null` was supplied), a new instance of [KotlinBuiltIns] will be created. The created built-ins
     * instance will be shared by all modules created in this method. But this instance will have no connection
     * with probably existing built-ins instance of your source module(s).
     */
    @Suppress("DEPRECATION_ERROR")
    fun createResolved(
        resolvedLibraries: KotlinLibraryResolveResult,
        storageManager: StorageManager,
        builtIns: KotlinBuiltIns?,
        languageVersionSettings: LanguageVersionSettings,
        friendModuleFiles: Set<org.jetbrains.kotlin.konan.file.File>,
        refinesModuleFiles: Set<org.jetbrains.kotlin.konan.file.File>,
        includedLibraryFiles: Set<org.jetbrains.kotlin.konan.file.File>,
        additionalDependencyModules: Iterable<ModuleDescriptorImpl>,
        isForMetadataCompilation: Boolean,
    ): KotlinResolvedModuleDescriptors

    /**
     * A duplicate of [createResolved], which accepts [java.nio.file.Path] instead of [org.jetbrains.kotlin.konan.file.File].
     *
     * FYI: No much attention to naming of this function, anyway it's going to be removed soon as a part of K1.
     */
    @Suppress("DEPRECATION_ERROR")
    fun createResolved2(
        resolvedLibraries: KotlinLibraryResolveResult,
        storageManager: StorageManager,
        builtIns: KotlinBuiltIns?,
        languageVersionSettings: LanguageVersionSettings,
        friendModuleFiles: Set<Path>,
        refinesModuleFiles: Set<Path>,
        includedLibraryFiles: Set<Path>,
        additionalDependencyModules: Iterable<ModuleDescriptorImpl>,
        isForMetadataCompilation: Boolean,
    ): KotlinResolvedModuleDescriptors = createResolved(
        resolvedLibraries = resolvedLibraries,
        storageManager = storageManager,
        builtIns = builtIns,
        languageVersionSettings = languageVersionSettings,
        friendModuleFiles = friendModuleFiles.mapTo(hashSetOf()) { org.jetbrains.kotlin.konan.file.File(it) },
        refinesModuleFiles = refinesModuleFiles.mapTo(hashSetOf()) { org.jetbrains.kotlin.konan.file.File(it) },
        includedLibraryFiles = includedLibraryFiles.mapTo(hashSetOf()) { org.jetbrains.kotlin.konan.file.File(it) },
        additionalDependencyModules = additionalDependencyModules,
        isForMetadataCompilation = isForMetadataCompilation,
    )
}

class KotlinResolvedModuleDescriptors(
    /**
     * The list of modules each representing an individual Kotlin/Native library. All modules
     * in this list have properly installed dependencies, i.e. module has all necessary dependencies
     * on other modules plus a dependency on the [forwardDeclarationsModule].
     */
    val resolvedDescriptors: List<ModuleDescriptorImpl>,

    /**
     * This is a module which "contains" forward declarations.
     * Note: this module should be unique per compilation and should always be the last dependency of any module.
     */
    val forwardDeclarationsModule: ModuleDescriptorImpl,

    val friendModules: Set<ModuleDescriptorImpl>,
    val refinesModules: Set<ModuleDescriptorImpl>
)
