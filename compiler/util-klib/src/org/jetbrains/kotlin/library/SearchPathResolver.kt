package org.jetbrains.kotlin.library

import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.library.SearchPathResolver.LookupResult
import org.jetbrains.kotlin.library.SearchPathResolver.SearchRoot
import org.jetbrains.kotlin.library.impl.createKotlinLibraryComponents
import org.jetbrains.kotlin.library.impl.isPre_1_4_Library
import org.jetbrains.kotlin.util.Logger
import org.jetbrains.kotlin.util.WithLogger
import org.jetbrains.kotlin.util.removeSuffixIfPresent
import java.nio.file.InvalidPathException
import java.nio.file.Paths

@Deprecated("Use KOTLIN_NATIVE_STDLIB_NAME, KOTLIN_JS_STDLIB_NAME or KOTLIN_WASM_STDLIB_NAME instead", level = DeprecationLevel.HIDDEN)
const val KOTLIN_STDLIB_NAME: String = "stdlib"

const val KOTLIN_NATIVE_STDLIB_NAME: String = "stdlib"
const val KOTLIN_JS_STDLIB_NAME: String = "kotlin"
const val KOTLIN_WASM_STDLIB_NAME: String = "kotlin"

interface SearchPathResolver<L : KotlinLibrary> : WithLogger {
    /**
     * The search root for KLIBs.
     *
     * @property searchRootPath The search root path.
     * @property allowLookupByRelativePath Whether the lookup by relative paths is allowed.
     *   true - yes, allow to look up by relative path and by library name.
     *   false - no, allow to look up strictly by library name.
     * @property isDeprecated Whether this search root is going to be removed in the future
     *   (and a warning should be displayed when a library was resolved against this root).
     */
    class SearchRoot(val searchRootPath: File, val allowLookupByRelativePath: Boolean = false, val isDeprecated: Boolean = false) {
        fun lookUp(libraryPath: File): LookupResult {
            if (libraryPath.isAbsolute) {
                // Look up by the absolute path if it is indeed an absolute path.
                return LookupResult.Found(lookUpByAbsolutePath(libraryPath) ?: return LookupResult.NotFound)
            }

            val isDefinitelyRelativePath = libraryPath.nameSegments.size > 1
            if (isDefinitelyRelativePath && !allowLookupByRelativePath) {
                // Lookup by the relative path is disallowed, but the path is definitely a relative path.
                return LookupResult.NotFound
            }

            // First, try to resolve by the relative path.
            val resolvedLibrary = lookUpByAbsolutePath(File(searchRootPath, libraryPath))
                ?: run {
                    if (!isDefinitelyRelativePath && libraryPath.extension.isEmpty()) {
                        // If the path actually looks like an unique name of the library, try to guess the name of the KLIB file.
                        // TODO: This logic is unreliable and needs to be replaced by the new KLIB resolver in the future.
                        lookUpByAbsolutePath(File(searchRootPath, "${libraryPath.path}.$KLIB_FILE_EXTENSION"))
                    } else null
                }
                ?: return LookupResult.NotFound

            return if (isDeprecated)
                LookupResult.FoundWithWarning(
                    library = resolvedLibrary,
                    warningText = "KLIB resolver: Library '${libraryPath.path}' was found in a custom library repository '${searchRootPath.path}'. " +
                            "Note, that custom library repositories are deprecated and will be removed in one of the future Kotlin releases. " +
                            "Please, avoid using '-repo' ('-r') compiler option and specify full paths to libraries in compiler CLI arguments."
                )
            else
                LookupResult.Found(resolvedLibrary)
        }

        companion object {
            fun lookUpByAbsolutePath(absoluteLibraryPath: File): File? =
                when {
                    absoluteLibraryPath.isFile -> {
                        // It's a really existing file.
                        when (absoluteLibraryPath.extension) {
                            KLIB_FILE_EXTENSION -> absoluteLibraryPath
                            "jar" -> {
                                // A special workaround for old JS stdlib, that was packed in a JAR file.
                                absoluteLibraryPath
                            }
                            else -> {
                                // A file with an unexpected extension.
                                null
                            }
                        }
                    }
                    absoluteLibraryPath.isDirectory -> {
                        // It's a really existing directory.
                        absoluteLibraryPath
                    }
                    else -> null
                }
        }
    }

    sealed interface LookupResult {
        object NotFound : LookupResult
        data class Found(val library: File) : LookupResult
        data class FoundWithWarning(val library: File, val warningText: String) : LookupResult
    }

    /**
     * The KLIB search roots:
     * 1. user working dir
     * 2. custom repositories (deprecated)
     * 3. the current K/N distribution
     *
     * IMPORTANT: The order of search roots actually defines the order of the lookup.
     */
    val searchRoots: List<SearchRoot>

    fun resolutionSequence(givenPath: String): Sequence<File>
    fun resolve(unresolved: LenientUnresolvedLibrary, isDefaultLink: Boolean = false): L?
    fun resolve(unresolved: RequiredUnresolvedLibrary, isDefaultLink: Boolean = false): L
    fun resolve(givenPath: String): L
    fun defaultLinks(noStdLib: Boolean, noDefaultLibs: Boolean, noEndorsedLibs: Boolean): List<L>
    fun libraryMatch(candidate: L, unresolved: UnresolvedLibrary): Boolean
    fun isProvidedByDefault(unresolved: UnresolvedLibrary): Boolean = false
}

fun <L : KotlinLibrary> SearchPathResolver<L>.resolve(unresolved: UnresolvedLibrary): L? = when (unresolved) {
    is LenientUnresolvedLibrary -> resolve(unresolved)
    is RequiredUnresolvedLibrary -> resolve(unresolved)
}

// This is a simple library resolver that only cares for file names.
abstract class KotlinLibrarySearchPathResolver<L : KotlinLibrary>(
    repositories: List<String>,
    directLibs: List<String>,
    val distributionKlib: String?,
    val localKotlinDir: String?,
    val skipCurrentDir: Boolean,
    override val logger: Logger
) : SearchPathResolver<L> {

    val localHead: File?
        get() = localKotlinDir?.File()?.klib

    val distHead: File?
        get() = distributionKlib?.File()?.child("common")

    open val distPlatformHead: File? = null

    val currentDirHead: File?
        get() = if (!skipCurrentDir) File.userDir else null

    private val repoRoots: List<File> by lazy { repositories.map { File(it) } }

    abstract fun libraryComponentBuilder(file: File, isDefault: Boolean): List<L>

    private val directLibraries: List<KotlinLibrary> by lazy {
        directLibs.mapNotNull { SearchRoot.lookUpByAbsolutePath(File(it)) }.flatMap { libraryComponentBuilder(it, false) }
    }

    override val searchRoots: List<SearchRoot> by lazy {
        val searchRoots = mutableListOf<SearchRoot?>()

        // Current working dir:
        searchRoots += currentDirHead?.let { SearchRoot(searchRootPath = it, allowLookupByRelativePath = true) }

        // Custom repositories (deprecated, to be removed):
        // TODO: remove after 2.0, KT-61098
        repositories.mapTo(searchRoots) { SearchRoot(searchRootPath = File(it), allowLookupByRelativePath = true, isDeprecated = true) }

        // Something likely never unused (deprecated, to be removed):
        // TODO: remove after 2.0, KT-61098
        searchRoots += localHead?.let { SearchRoot(searchRootPath = it, allowLookupByRelativePath = true, isDeprecated = true) }

        // Current Kotlin/Native distribution:
        searchRoots += distHead?.let { SearchRoot(searchRootPath = it) }
        searchRoots += distPlatformHead?.let { SearchRoot(searchRootPath = it) }

        searchRoots.filterNotNull()
    }

    /**
     * Returns a [File] instance if the [path] is valid on the current file system and null otherwise.
     * Doesn't check whether the file denoted by [path] really exists.
     */
    private fun validFileOrNull(path: String): File? =
        try {
            File(Paths.get(path))
        } catch (_: InvalidPathException) {
            null
        }

    /**
     * Returns a sequence of libraries passed to the compiler directly for which unique_name == [givenName].
     */
    private fun directLibsSequence(givenName: String): Sequence<File> {
        // Search among user-provided libraries by unique name.
        // It's a workaround for maven publication. When a library is published without Gradle metadata,
        // it has a complex file name (e.g. foo-macos_x64-1.0.klib). But a dependency on this lib in manifests
        // of other libs uses its unique name written in the manifest (i.e just 'foo'). So we cannot resolve this
        // library by its filename. But we have this library's file (we've downloaded it using maven dependency
        // resolution) so we can pass it to the compiler directly. This code takes this into account and looks for
        // a library dependencies also in libs passed to the compiler as files (passed to the resolver as the
        // 'directLibraries' property).
        return directLibraries.asSequence().filter {
            it.uniqueName == givenName
        }.map {
            it.libraryFile
        }
    }

    override fun resolutionSequence(givenPath: String): Sequence<File> {
        val given: File? = validFileOrNull(givenPath)
        val sequence: Sequence<File?> = when {
            given == null -> {
                // The given path can't denote a real file, so just look for such
                // unique_name among libraries passed to the compiler directly.
                directLibsSequence(givenPath)
            }
            given.isAbsolute ->
                sequenceOf(SearchRoot.lookUpByAbsolutePath(given))
            else -> {
                // Search among libraries in repositories by library filename.
                val repoLibs = searchRoots.asSequence().map { searchRoot ->
                    when (val lookupResult = searchRoot.lookUp(given)) {
                        is LookupResult.Found -> lookupResult.library
                        is LookupResult.FoundWithWarning -> {
                            logger.warning(lookupResult.warningText)
                            lookupResult.library
                        }
                        LookupResult.NotFound -> null
                    }
                }
                // The given path still may denote a unique name of a direct library.
                directLibsSequence(givenPath) + repoLibs
            }
        }
        return sequence.filterNotNull()
    }

    private fun Sequence<File>.filterOutPre_1_4_libraries(): Sequence<File> = this.filter {
        if (it.isPre_1_4_Library) {
            logger.warning("KLIB resolver: Skipping '$it'. This is a pre 1.4 library.")
            false
        } else {
            true
        }
    }

    // Default libraries could be resolved several times during findLibraries and resolveDependencies.
    // Store already resolved libraries.
    private inner class ResolvedLibrary(val library: L?)

    private val resolvedLibraries = HashMap<UnresolvedLibrary, ResolvedLibrary>()

    private fun resolveOrNull(unresolved: UnresolvedLibrary, isDefaultLink: Boolean): L? {
        return resolvedLibraries.getOrPut(unresolved) {
            val givenPath = unresolved.path
            try {
                resolutionSequence(givenPath)
                    .filterOutPre_1_4_libraries()
                    .flatMap { libraryComponentBuilder(it, isDefaultLink).asSequence() }
                    .map { it.takeIf { libraryMatch(it, unresolved) } }
                    .filterNotNull()
                    .firstOrNull()
                    .let(::ResolvedLibrary)
            } catch (e: Throwable) {
                logger.error("KLIB resolver: Failed to resolve Kotlin library: $givenPath")
                throw e
            }
        }.library
    }

    override fun resolve(unresolved: LenientUnresolvedLibrary, isDefaultLink: Boolean): L? {
        return resolveOrNull(unresolved, isDefaultLink)
    }

    override fun resolve(unresolved: RequiredUnresolvedLibrary, isDefaultLink: Boolean): L {
        return resolveOrNull(unresolved, isDefaultLink)
            ?: run {
                // It does not make sense to replace logger.fatal() by logger.error() here, because:
                // 1. We don't know which exactly side effect (throwing an exception, exiting JVM process, etc) is performed
                //    by logger.fatal() as we don't know the concrete Logger class. So, we can't use logger.error()
                //    and emulate the proper side effect here.
                // 2. The contract of resolve(RequiredUnresolvedLibrary, Boolean) has a design flaw: It assumes that either
                //    the library is resolved or the side effect takes place. The latter (side effect) affects the execution
                //    flow of the program and should not be a responsibility of SearchPathResolver.
                // 3. Finally, we are going to drop SearchPathResolver which is a part of KLIB resolver.
                @Suppress("DEPRECATION")
                logger.fatal("KLIB resolver: Could not find \"${unresolved.path}\" in ${searchRoots.map { it.searchRootPath.absolutePath }}")
            }
    }

    override fun libraryMatch(candidate: L, unresolved: UnresolvedLibrary): Boolean = true

    override fun resolve(givenPath: String) = resolve(UnresolvedLibrary(givenPath, null), false)

    private val File.klib
        get() = File(this, "klib")

    // The libraries from the default root are linked automatically.
    val defaultRoots: List<File>
        get() = listOfNotNull(distHead, distPlatformHead).filter { it.exists }

    private fun getDefaultLibrariesFromDir(directory: File, prefix: String = "org.jetbrains.kotlin") =
        if (directory.exists) {
            directory.listFiles
                .asSequence()
                .filter { it.name.startsWith(prefix) }
                .filterNot { it.name.startsWith('.') }
                .filterNot { it.name.removeSuffixIfPresent(KLIB_FILE_EXTENSION_WITH_DOT) == KOTLIN_NATIVE_STDLIB_NAME }
                .map { UnresolvedLibrary(it.absolutePath, null) }
                .map { resolve(it, isDefaultLink = true) }
        } else emptySequence()

    override fun defaultLinks(noStdLib: Boolean, noDefaultLibs: Boolean, noEndorsedLibs: Boolean): List<L> {

        val result = mutableListOf<L>()

        if (!noStdLib) {
            result.add(resolve(UnresolvedLibrary(KOTLIN_NATIVE_STDLIB_NAME, null), true))
        }

        // Endorsed libraries in distHead.
        if (!noEndorsedLibs) {
            distHead?.let {
                result.addAll(getDefaultLibrariesFromDir(it))
            }
        }
        // Platform libraries resolve.
        if (!noDefaultLibs) {
            distPlatformHead?.let {
                result.addAll(getDefaultLibrariesFromDir(it))
            }
        }

        return result
    }
}

// This is a library resolver aware of attributes shared between platforms,
// such as abi version.
// JS and Native resolvers are inherited from this one.
abstract class KotlinLibraryProperResolverWithAttributes<L : KotlinLibrary>(
    repositories: List<String>,
    directLibs: List<String>,
    distributionKlib: String?,
    localKotlinDir: String?,
    skipCurrentDir: Boolean,
    override val logger: Logger,
    private val knownIrProviders: List<String>
) : KotlinLibrarySearchPathResolver<L>(repositories, directLibs, distributionKlib, localKotlinDir, skipCurrentDir, logger),
    SearchPathResolver<L> {
    override fun libraryMatch(candidate: L, unresolved: UnresolvedLibrary): Boolean {
        val candidatePath = candidate.libraryFile.absolutePath

        val candidateCompilerVersion = candidate.versions.compilerVersion
        val candidateAbiVersion = candidate.versions.abiVersion
        val candidateLibraryVersion = candidate.versions.libraryVersion

        // Rejecting a library at this stage has disadvantages - the diagnostics are not-understandable.
        // Please, don't add checks for other versions here. For example, check for the metadata version should be
        // implemented in KlibDeserializedContainerSource.incompatibility
        if (candidateAbiVersion?.isCompatible() != true) {
            logger.warning("KLIB resolver: Skipping '$candidatePath'. Incompatible ABI version. The current default is '${KotlinAbiVersion.CURRENT}', found '${candidateAbiVersion}'. The library was produced by '$candidateCompilerVersion' compiler.")
            return false
        }

        if (candidateLibraryVersion != unresolved.libraryVersion &&
            candidateLibraryVersion != null &&
            unresolved.libraryVersion != null
        ) {
            logger.warning("KLIB resolver: Skipping '$candidatePath'. Library versions don't match. Expected '${unresolved.libraryVersion}', found '${candidateLibraryVersion}'.")
            return false
        }

        candidate.irProviderName?.let {
            if (it !in knownIrProviders) {
                logger.warning("KLIB resolver: Skipping '$candidatePath'. The library requires unknown IR provider: $it")
                return false
            }
        }

        return true
    }
}

class SingleKlibComponentResolver(
    klibFile: String,
    logger: Logger,
    knownIrProviders: List<String>
) : KotlinLibraryProperResolverWithAttributes<KotlinLibrary>(
    emptyList(), listOf(klibFile),
    null, null, false, logger, knownIrProviders
) {
    override fun libraryComponentBuilder(file: File, isDefault: Boolean) = createKotlinLibraryComponents(file, isDefault)
}

/**
 * Resolves KLIB libraries by:
 * - expanding the given library path to the real path that may or may not contain ".klib" extension
 * - searching among user-supplied libraries by "unique_name" that matches the given library name
 * - filtering out pre-1.4 libraries (with the old style layout)
 * - filtering out library components that have different ABI version than the ABI version of the current compiler
 * - filtering out libraries with non-default ir_provider.
 *
 * If no match found, fails with [Logger#fatal].
 *
 * Typical usage scenario: compiler.
 */
object CompilerSingleFileKlibResolveStrategy : SingleFileKlibResolveStrategy {
    override fun resolve(libraryFile: File, logger: Logger) =
        SingleKlibComponentResolver(
            libraryFile.absolutePath, logger, emptyList()
        ).resolve(libraryFile.absolutePath)
}

/**
 * Similar to [CompilerSingleFileKlibResolveStrategy], but doesn't filter out
 * libraries with [knownIrProviders].
 */
// TODO: It looks like a hack because it is.
//  The reason this strategy exists is that we shouldn't skip Native metadata-based interop libraries
//  when generating compiler caches.
class CompilerSingleFileKlibResolveAllowingIrProvidersStrategy(
    private val knownIrProviders: List<String>
) : SingleFileKlibResolveStrategy {
    override fun resolve(libraryFile: File, logger: Logger) =
        SingleKlibComponentResolver(
            libraryFile.absolutePath, logger, knownIrProviders
        ).resolve(libraryFile.absolutePath)
}
