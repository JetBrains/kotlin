@file:Suppress("FunctionName")

package org.jetbrains.kotlin.library

// TODO: drop this as a part of KLIB resolver (+)
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
// TODO: drop this as a part of KLIB resolver (+)
sealed class UnresolvedLibrary {
    abstract val path: String
}

// TODO: drop this as a part of KLIB resolver (+)
data class RequiredUnresolvedLibrary(override val path: String) : UnresolvedLibrary()

// TODO: drop this as a part of KLIB resolver (+)
data class LenientUnresolvedLibrary(override val path: String) : UnresolvedLibrary()
