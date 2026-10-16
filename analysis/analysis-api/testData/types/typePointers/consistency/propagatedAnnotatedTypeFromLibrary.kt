// MODULE: library
// MODULE_KIND: LibraryBinary
// FILE: library.kt
package library

@Target(AnnotationTarget.TYPE)
annotation class Anno

@Target(AnnotationTarget.TYPE)
annotation class AnnoWithArgs(val x: String)

val i: @Anno @AnnoWithArgs("") Int = 1

// MODULE: main(library)
// MODULE_KIND: Source
// FILE: main.kt
import library.i

fun foo() {
    val iCopy = i
    <expr>iCopy</expr>
}
