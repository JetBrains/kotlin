// IGNORE_STANDALONE
// Standalone mode doesn't provide PSI for declarations from binary libraries
// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: lib.kt
package lib

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_GETTER)
annotation class Anno

@Anno
@get:Anno
val <T> List<T>.bar: Map<Int, T>
    get() = TODO()

// MODULE: main(lib)
// FILE: main.kt
import lib.bar

fun test(list: List<String>) {
    list.b<caret>ar
}
