@file:Suppress("FunctionName")

package org.jetbrains.kotlin.library

@Deprecated(DEPRECATED_LIBRARY_AND_DEPENDENCY_VERSIONS, ReplaceWith("RequiredUnresolvedLibrary(path)"), level = DeprecationLevel.ERROR)
fun UnresolvedLibrary(path: String, @Suppress("UNUSED_PARAMETER") libraryVersion: String?): RequiredUnresolvedLibrary =
    RequiredUnresolvedLibrary(path)

@Deprecated(DEPRECATED_LIBRARY_AND_DEPENDENCY_VERSIONS, ReplaceWith("UnresolvedLibrary(path, lenient)"), level = DeprecationLevel.ERROR)
fun UnresolvedLibrary(path: String, @Suppress("UNUSED_PARAMETER") libraryVersion: String?, lenient: Boolean): UnresolvedLibrary =
    if (lenient) LenientUnresolvedLibrary(path) else RequiredUnresolvedLibrary(path)

fun UnresolvedLibrary(path: String, lenient: Boolean): UnresolvedLibrary =
    if (lenient) LenientUnresolvedLibrary(path) else RequiredUnresolvedLibrary(path)

/**
 * Representation of a Kotlin library that has not been yet resolved.
 *
 * TODO: This class has a major design flaw and needs to be replaced by the new KLIB resolver in the future.
 * - In certain situations [path] represents a path to the library, would it be relative or absolute.
 * - In certain situations [path] represents an `unique_name` of the library.
 * - In general, `unique_name` needs not be equal to the file name of the library. And this adds some mess to the classes
 *   that implement the "resolver" logic, e.g. [SearchPathResolver].
 */
sealed class UnresolvedLibrary {
    abstract val path: String

    @Suppress("DeprecatedCallableAddReplaceWith") // There is no replacement.
    @Deprecated(DEPRECATED_LIBRARY_AND_DEPENDENCY_VERSIONS, level = DeprecationLevel.ERROR)
    val libraryVersion: String? get() = null

    @Deprecated(DEPRECATED_SUBSTITUTE_PATH, level = DeprecationLevel.ERROR)
    abstract fun substitutePath(newPath: String): UnresolvedLibrary

    companion object {
        const val DEPRECATED_SUBSTITUTE_PATH =
            "UnresolvedLibrary.substitutePath() is deprecated and is going to be removed in one of the future Kotlin releases"
    }
}

data class RequiredUnresolvedLibrary(
    override val path: String,
) : UnresolvedLibrary() {
    @Deprecated(DEPRECATED_LIBRARY_AND_DEPENDENCY_VERSIONS, level = DeprecationLevel.ERROR)
    constructor(path: String, @Suppress("UNUSED_PARAMETER") libraryVersion: String?) : this(path)

    @Deprecated(DEPRECATED_SUBSTITUTE_PATH, ReplaceWith("copy(path = newPath)"), level = DeprecationLevel.ERROR)
    override fun substitutePath(newPath: String): RequiredUnresolvedLibrary {
        return copy(path = newPath)
    }
}

data class LenientUnresolvedLibrary(
    override val path: String,
) : UnresolvedLibrary() {
    @Deprecated(DEPRECATED_LIBRARY_AND_DEPENDENCY_VERSIONS, level = DeprecationLevel.ERROR)
    constructor(path: String, @Suppress("UNUSED_PARAMETER") libraryVersion: String?) : this(path)

    @Deprecated(DEPRECATED_SUBSTITUTE_PATH, ReplaceWith("copy(path = newPath)"), level = DeprecationLevel.ERROR)
    override fun substitutePath(newPath: String): LenientUnresolvedLibrary {
        return copy(path = newPath)
    }
}
