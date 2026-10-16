// MODULE: library
// MODULE_KIND: LibraryBinary
// FILE: library.kt
package library

@Target(AnnotationTarget.TYPE)
annotation class Anno(val number: Int)

const val value = 0

fun typeWithAnnotation(): @Anno(value) String = ""

var resolveMe
    get() = typeWithAnnotation()
    set(value) {}

// MODULE: main(library)
// MODULE_KIND: Source
// FILE: main.kt
import library.resolveMe

fun foo() {
    val x = <expr>resolveMe</expr>
}
