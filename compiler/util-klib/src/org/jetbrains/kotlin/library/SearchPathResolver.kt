package org.jetbrains.kotlin.library

import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.library.SearchPathResolver.LookupResult
import org.jetbrains.kotlin.library.SearchPathResolver.SearchRoot
import org.jetbrains.kotlin.library.impl.createKotlinLibraryComponents
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
const val KOTLINTEST_MODULE_NAME: String = "kotlin-test"

interface SearchPathResolver<L : KotlinLibrary> : WithLogger {
    /**
     * The search root for KLIBs.
     *
     * @property searchRootPath The search root path.
     * @property allowLookupByRelativePath Whether the lookup by relative paths is allowed.
     *   true - yes, allow to look up by relative path and by library name.
     *   false - no, allow to look up strictly by library name.
     */
    class SearchRoot(val searchRootPath: File, val allowLookupByRelativePath: Boolean = false) {
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

            return LookupResult.Found(resolvedLibrary)
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
    directLibs: List<String>,
    val distributionKlib: String?,
    private val skipCurrentDir: Boolean,
    override val logger: Logger
) : SearchPathResolver<L> {

    private val distHead: File? get() = distributionKlib?.File()?.child("common")
    open val distPlatformHead: File? = null
    private val currentDirHead: File? get() = if (!skipCurrentDir) File.userDir else null

    abstract fun libraryComponentBuilder(file: File, isDefault: Boolean): List<L>

    private val directLibraryUniqueNames: Map</* Unique Name*/ String, File> by lazy {
        HashMap<String, File>().apply {
            for (directLib in directLibs) {
                val absolutePath = SearchRoot.lookUpByAbsolutePath(File(directLib)) ?: continue
                val uniqueName = libraryComponentBuilder(absolutePath, false).singleOrNull()?.uniqueName ?: continue
                this[uniqueName] = absolutePath
            }
        }
    }

    override val searchRoots: List<SearchRoot> by lazy {
        val searchRoots = mutableListOf<SearchRoot?>()

        // Current working dir:
        searchRoots += currentDirHead?.let { SearchRoot(searchRootPath = it, allowLookupByRelativePath = true) }

        // Current Kotlin/Native distribution:
        searchRoots += distHead?.let { SearchRoot(searchRootPath = it) }
        searchRoots += distPlatformHead?.let { SearchRoot(searchRootPath = it) }

        searchRoots.filterNotNull()
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
        return generateSequence { directLibraryUniqueNames[givenName] }
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
                        LookupResult.NotFound -> null
                    }
                }
                // The given path still may denote a unique name of a direct library.
                directLibsSequence(givenPath) + repoLibs
            }
        }
        return sequence.filterNotNull()
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
                    .flatMap { libraryComponentBuilder(it, isDefaultLink).asSequence() }
                    .map { it.takeIf { libraryMatch(it, unresolved) } }
                    .filterNotNull()
                    .firstOrNull()
                    .let(::ResolvedLibrary)
            } catch (e: Throwable) {
                logger.error("KLIB resolver: Failed to resolve Kotlin library: $givenPath, due to ${e.message}")
                throw e
            }
        }.library
    }

    override fun resolve(unresolved: LenientUnresolvedLibrary, isDefaultLink: Boolean): L? {
        return resolveOrNull(unresolved, isDefaultLink)
            ?: run {
                logger.warning("KLIB resolver: Could not find \"${unresolved.path}\" in ${searchRoots.map { it.searchRootPath.absolutePath }}")
                null
            }
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

    override fun resolve(givenPath: String) = resolve(RequiredUnresolvedLibrary(givenPath), false)

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
                .map { RequiredUnresolvedLibrary(it.absolutePath) }
                .map { resolve(it, isDefaultLink = true) }
        } else emptySequence()

    override fun defaultLinks(noStdLib: Boolean, noDefaultLibs: Boolean, noEndorsedLibs: Boolean): List<L> {

        val result = mutableListOf<L>()

        if (!noStdLib) {
            result.add(resolve(RequiredUnresolvedLibrary(KOTLIN_NATIVE_STDLIB_NAME), true))
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
    directLibs: List<String>,
    distributionKlib: String?,
    skipCurrentDir: Boolean,
    logger: Logger,
    private val knownIrProviders: List<String>
) : KotlinLibrarySearchPathResolver<L>(directLibs, distributionKlib, skipCurrentDir, logger), SearchPathResolver<L> {

    @Deprecated(
        "Please use the KotlinLibraryProperResolverWithAttributes constructor which does not has 'repositories' and 'localKotlinDir' value parameters",
        ReplaceWith("KotlinLibraryProperResolverWithAttributes<L>(directLibs, distributionKlib, skipCurrentDir, logger, knownIrProviders)"),
    )
    constructor(
        @Suppress("UNUSED_PARAMETER") repositories: List<String>,
        directLibs: List<String>,
        distributionKlib: String?,
        @Suppress("UNUSED_PARAMETER") localKotlinDir: String?,
        skipCurrentDir: Boolean,
        logger: Logger,
        knownIrProviders: List<String>
    ) : this(directLibs, distributionKlib, skipCurrentDir, logger, knownIrProviders)

    override fun libraryMatch(candidate: L, unresolved: UnresolvedLibrary): Boolean {
        val candidatePath = candidate.libraryFile.absolutePath

        val candidateCompilerVersion = candidate.versions.compilerVersion
        val candidateAbiVersion = candidate.versions.abiVersion

        // Rejecting a library at this stage has disadvantages - the diagnostics are not-understandable.
        // Please, don't add checks for other versions here. For example, check for the metadata version should be
        // implemented in KlibDeserializedContainerSource.incompatibility
        if (candidateAbiVersion?.isCompatible() != true) {
            logger.strongWarning("KLIB resolver: Skipping '$candidatePath' having incompatible ABI version '${candidateAbiVersion}'. " +
                        "The library was produced by '$candidateCompilerVersion' compiler.\n" +
                        "The current Kotlin compiler can consume libraries having ABI version <= '${KotlinAbiVersion.CURRENT}'.\n" +
                        "Please upgrade your Kotlin compiler version to consume this library.")
            return false
        }

        candidate.irProviderName?.let {
            if (it !in knownIrProviders) {
                logger.strongWarning("KLIB resolver: Skipping '$candidatePath'. The library requires unknown IR provider: $it")
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
    directLibs = listOf(klibFile),
    distributionKlib = null,
    skipCurrentDir = false,
    logger,
    knownIrProviders = knownIrProviders
) {
    override fun libraryComponentBuilder(file: File, isDefault: Boolean) = createKotlinLibraryComponents(file, isDefault)
}

/**
 * Resolves KLIB libraries by:
 * - expanding the given library path to the real path that may or may not contain ".klib" extension
 * - searching among user-supplied libraries by "unique_name" that matches the given library name
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

/**
 * Note: The chosen approach is a kind of hack: Instead of doing honest
 * check inside KLIB resolver if a unique name was passed as one of
 * user-defined libs (e.g., via `-library` or `-l` CLI argument), we do
 * post-check. The post-check runs after the KLIB resolver, and just
 * makes sure that every item that has been passed as a user-defined lib
 * and looks like a unique name actually matches the file name of the KLIB.
 *
 * This approach works good enough. And it is much simpler to implement
 * than implementing the "honest" check. Anyway, it's a temporary
 * solution that will exist until we replace the KLIB resolved by
 * a completely new, much simpler and well-tested component.
 *
 * Note: This check may give false positives in case unique name
 * includes path separator character. We consider this as an ultimately
 * rare case, and accept the possible risks.
 */
fun validateNoLibrariesWerePassedViaCliByUniqueName(givenNames: List<String>, resolvedLibraries: List<KotlinLibrary>, logger: Logger) {
    if (givenNames.isEmpty() || resolvedLibraries.isEmpty()) return

    val potentiallyUniqueNames: Set<String> = givenNames.filterTo(hashSetOf()) { givenName ->
        val given: File? = validFileOrNull(givenName)
        given == null || given.nameSegments.size == 1
    }

    if (potentiallyUniqueNames.isEmpty()) return

    for (library in resolvedLibraries) {
        val uniqueName = library.uniqueName
        if (uniqueName in potentiallyUniqueNames && uniqueName != library.libraryFile.name) {
            // The name of the library file or the library directory does not match the unique name,
            // and at the same time this unique name was specified at CLI argument.
            // Need to report an error.
            logger.error(
                "KLIB resolver: Library '${library.libraryFile}' was found by its unique name '$uniqueName'. " +
                        "This could happen if the library unique name was passed instead of the library path via the compiler CLI argument `-library` (`-l`). " +
                        "Note that using unique name is deprecated and will become unavailable in one of the future Kotlin releases. " +
                        "Please, specify full paths to libraries in compiler CLI arguments."
            )
        }
    }
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

