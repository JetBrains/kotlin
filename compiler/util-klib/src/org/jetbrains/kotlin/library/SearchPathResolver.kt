package org.jetbrains.kotlin.library

import org.jetbrains.kotlin.konan.CompilerVersion
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.library.impl.isPre_1_4_Library
import org.jetbrains.kotlin.util.*

const val KOTLIN_STDLIB_NAME = "stdlib"

interface SearchPathResolver<L: KotlinLibrary> : WithLogger {
    val searchRoots: List<File>
    fun resolutionSequence(givenPath: String): Sequence<File>
    fun resolve(unresolved: UnresolvedLibrary, isDefaultLink: Boolean = false): L
    fun resolve(givenPath: String): L
    fun defaultLinks(noStdLib: Boolean, noDefaultLibs: Boolean, noEndorsedLibs: Boolean): List<L>
    fun libraryMatch(candidate: L, unresolved: UnresolvedLibrary): Boolean
}

interface SearchPathResolverWithAttributes<L: KotlinLibrary>: SearchPathResolver<L> {
    val knownAbiVersions: List<KotlinAbiVersion>?
    val knownCompilerVersions: List<CompilerVersion>?
}

// This is a simple library resolver that only cares for file names.
abstract class KotlinLibrarySearchPathResolver<L: KotlinLibrary> (
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

    private fun found(candidate: File): File? {
        fun check(file: File): Boolean = file.exists

        val noSuffix = File(candidate.path.removeSuffixIfPresent(KLIB_FILE_EXTENSION_WITH_DOT))
        val withSuffix = File(candidate.path.suffixIfNot(KLIB_FILE_EXTENSION_WITH_DOT))
        return when {
            check(withSuffix) -> withSuffix
            check(noSuffix) -> noSuffix
            else -> null
        }
    }

    override fun resolutionSequence(givenPath: String): Sequence<File> {
        val given = File(givenPath)
        val sequence = if (given.isAbsolute) {
            sequenceOf(found(given))
        } else {
            // Search among user-provided libraries by unique name.
            // It's a workaround for maven publication. When a library is published without Gradle metadata,
            // it has a complex file name (e.g. foo-macos_x64-1.0.klib). But a dependency on this lib in manifests
            // of other libs uses its unique name written in the manifest (i.e just 'foo'). So we cannot resolve this
            // library by its filename. But we have this library's file (we've downloaded it using maven dependency
            // resolution) so we can pass it to the compiler directly. This code takes this into account and looks for
            // a library dependencies also in libs passed to the compiler as files (passed to the resolver as the
            // 'directLibraries' property).
            val directLibs = directLibraries.asSequence().filter {
                it.uniqueName == givenPath
            }.map {
                it.libraryFile
            }
            // Search among libraries in repositoreis by library filename.
            val repoLibs = searchRoots.asSequence().map {
                found(File(it, givenPath))
            }
            directLibs + repoLibs
        }
        return sequence.filterNotNull()
    }

    private fun Sequence<File>.filterOutPre_1_4_libraries(): Sequence<File>  = this.filter{
            if (it.isPre_1_4_Library) {
                logger.warning("Skipping \"$it\" as it is a pre 1.4 library")
                false
            } else {
                true
            }
        }

    override fun resolve(unresolved: UnresolvedLibrary, isDefaultLink: Boolean): L {
        val givenPath = unresolved.path
        try {
            val fileSequence = resolutionSequence(givenPath)
            val matching = fileSequence
                .filterOutPre_1_4_libraries()
                .flatMap { libraryComponentBuilder(it, isDefaultLink).asSequence() }
                .map { it.takeIf { libraryMatch(it, unresolved) } }
                .filterNotNull()

            return matching.firstOrNull() ?: run {
                logger.fatal("Could not find \"$givenPath\" in ${searchRoots.map { it.absolutePath }}.")
            }
        } catch (e: Throwable) {
            logger.error("Failed to resolve Kotlin library: $givenPath")
            throw e
        }
    }

    override fun libraryMatch(candidate: L, unresolved: UnresolvedLibrary) = true

    override fun resolve(givenPath: String) = resolve(UnresolvedLibrary(givenPath, null), false)

    private val File.klib
        get() = File(this, "klib")

    // The libraries from the default root are linked automatically.
    val defaultRoots: List<File>
        get() = listOfNotNull(distHead, distPlatformHead).filter { it.exists }

    private fun getDefaultLibrariesFromDir(directory: File) =
        if (directory.exists) {
            directory.listFiles
                .asSequence()
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

fun CompilerVersion.compatible(other: CompilerVersion) =
        this.major == other.major
        && this.minor == other.minor
        && this.maintenance == other.maintenance


// This is a library resolver aware of attributes shared between platforms,
// such as abi version.
// JS and Native resolvers are inherited from this one.
abstract class KotlinLibraryProperResolverWithAttributes<L: KotlinLibrary>(
    repositories: List<String>,
    directLibs: List<String>,
    override val knownAbiVersions: List<KotlinAbiVersion>?,
    override val knownCompilerVersions: List<CompilerVersion>?,
    distributionKlib: String?,
    localKotlinDir: String?,
    skipCurrentDir: Boolean,
    override val logger: Logger,
    private val knownIrProviders: List<String>
) : KotlinLibrarySearchPathResolver<L>(repositories, directLibs, distributionKlib, localKotlinDir, skipCurrentDir, logger),
    SearchPathResolverWithAttributes<L>
{
    override fun libraryMatch(candidate: L, unresolved: UnresolvedLibrary): Boolean {
        val candidatePath = candidate.libraryFile.absolutePath

        val candidateCompilerVersion = candidate.versions.compilerVersion
        val candidateAbiVersion = candidate.versions.abiVersion
        val candidateLibraryVersion = candidate.versions.libraryVersion


        val abiVersionMatch = candidateAbiVersion != null &&
                knownAbiVersions != null &&
                knownAbiVersions!!.contains(candidateAbiVersion)

        if (!abiVersionMatch) {
            logger.warning("skipping $candidatePath. The abi versions don't match. Expected '${knownAbiVersions}', found '${candidateAbiVersion}'")
            return false
        }

        if (candidateLibraryVersion != unresolved.libraryVersion &&
            candidateLibraryVersion != null &&
            unresolved.libraryVersion != null
        ) {
            logger.warning("skipping $candidatePath. The library versions don't match. Expected '${unresolved.libraryVersion}', found '${candidateLibraryVersion}'")
            return false
        }

        candidate.manifestProperties["ir_provider"]?.let {
            if (it !in knownIrProviders) {
                logger.warning("skipping $candidatePath. The library requires unknown IR provider $it.")
                return false
            }
        }

        return true
    }
}