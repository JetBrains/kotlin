// LANGUAGE: +JvmEnhancedBridges

// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: Lib.kt
package lib

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class SourceOnly

interface A<T> {
    @SourceOnly
    @Throws(java.io.IOException::class)
    fun get(): T
}

// MODULE: main(lib)
// FILE: main.kt
import lib.A

class B : A<String> {
    override fun get(): String = "OK"
}

fun usage() {
    val instance: A<String> = B()
    instance.<caret>get()
}
