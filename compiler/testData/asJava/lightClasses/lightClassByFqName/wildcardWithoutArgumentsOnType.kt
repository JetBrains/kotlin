// One
// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

open class One {
    protected open fun beforeResolve(files: SomeInterface<@JvmSuppressWildcards SomeClass>): Boolean {
        return true
    }
}

interface SomeInterface<out T>
open class SomeClass