package org.jetbrains.kotlin.library

import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.library.impl.createKotlinLibraryComponents
import org.jetbrains.kotlin.library.impl.isPre_1_4_Library
import org.jetbrains.kotlin.util.Logger
import org.jetbrains.kotlin.util.WithLogger
import org.jetbrains.kotlin.util.removeSuffixIfPresent
import org.jetbrains.kotlin.util.suffixIfNot
import java.nio.file.InvalidPathException
import java.nio.file.Paths

const val KOTLIN_STDLIB_NAME = "stdlib"

interface SearchPathResolver<L : KotlinLibrary> : WithLogger {
    val searchRoots: List<File>
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
        directLibs.mapNotNull { found(File(it)) }.flatMap { libraryComponentBuilder(it, false) }
    }

    // This is the place where we specify the order of library search.
    override val searchRoots: List<File> by lazy {
        (listOf(currentDirHead) + repoRoots + listOf(localHead, distHead, distPlatformHead)).filterNotNull()
    }

    private val files: Set<String> by lazy { searchRoots.flatMap { it.listFilesOrEmpty }.map { it.absolutePath }.toSet() }

    private fun found(candidate: File): File? {
        fun check(file: File): Boolean = files.contains(file.absolutePath) || file.exists

        val noSuffix = File(candidate.path.removeSuffixIfPresent(KLIB_FILE_EXTENSION_WITH_DOT))
        val withSuffix = File(candidate.path.suffixIfNot(KLIB_FILE_EXTENSION_WITH_DOT))
        return when {
            check(withSuffix) -> withSuffix
            check(noSuffix) -> noSuffix
            else -> null
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
        val given = validFileOrNull(givenPath)
        val sequence = when {
            given == null -> {
                // The given path can't denote a real file, so just look for such
                // unique_name among libraries passed to the compiler directly.
                directLibsSequence(givenPath)
            }
            given.isAbsolute ->
                sequenceOf(found(given))
            else -> {
                // Search among libraries in repositories by library filename.
                val repoLibs = searchRoots.asSequence().map {
                    found(File(it, given))
                }
                // The given path still may denote a unique name of a direct library.
                directLibsSequence(givenPath) + repoLibs
            }
        }
        return sequence.filterNotNull()
    }

    private fun Sequence<File>.filterOutPre_1_4_libraries(): Sequence<File> = this.filter {
        if (it.isPre_1_4_Library) {
            logger.warning("Skipping \"$it\" as it is a pre 1.4 library")
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
                logger.error("Failed to resolve Kotlin library: $givenPath")
                throw e
            }
        }.library
    }

    override fun resolve(unresolved: LenientUnresolvedLibrary, isDefaultLink: Boolean): L? {
        return resolveOrNull(unresolved, isDefaultLink)
    }

    override fun resolve(unresolved: RequiredUnresolvedLibrary, isDefaultLink: Boolean): L {
        return resolveOrNull(unresolved, isDefaultLink)
            ?: logger.fatal("Could not find \"${unresolved.path}\" in ${searchRoots.map { it.absolutePath }}")
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
                .filterNot { it.name.removeSuffixIfPresent(KLIB_FILE_EXTENSION_WITH_DOT) == KOTLIN_STDLIB_NAME }
                .map { UnresolvedLibrary(it.absolutePath, null) }
                .map { resolve(it, isDefaultLink = true) }
        } else emptySequence()

    override fun defaultLinks(noStdLib: Boolean, noDefaultLibs: Boolean, noEndorsedLibs: Boolean): List<L> {

        val result = mutableListOf<L>()

        if (!noStdLib) {
            result.add(resolve(UnresolvedLibrary(KOTLIN_STDLIB_NAME, null), true))
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
            logger.warning("skipping $candidatePath. Incompatible abi version. The current default is '${KotlinAbiVersion.CURRENT}', found '${candidateAbiVersion}'. The library produced by ${candidateCompilerVersion} compiler")
            return false
        }

        if (candidateLibraryVersion != unresolved.libraryVersion &&
            candidateLibraryVersion != null &&
            unresolved.libraryVersion != null
        ) {
            logger.warning("skipping $candidatePath. The library versions don't match. Expected '${unresolved.libraryVersion}', found '${candidateLibraryVersion}'")
            return false
        }

        candidate.irProviderName?.let {
            if (it !in knownIrProviders) {
                logger.warning("skipping $candidatePath. The library requires unknown IR provider $it.")
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
