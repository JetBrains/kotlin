// One
// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

open class One {
    @JvmSuppressWildcards
    protected open fun beforeResolve(files: SomeInterface<SomeClass>): Boolean {
        return true
    }
}

interface SomeInterface<out T>
open class SomeClass