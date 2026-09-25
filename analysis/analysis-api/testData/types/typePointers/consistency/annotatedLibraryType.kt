// MODULE: library
// MODULE_KIND: LibraryBinary
// FILE: library.kt
package library

@Target(AnnotationTarget.TYPE)
annotation class Anno

@Target(AnnotationTarget.TYPE)
annotation class AnnoWithArgs(val x: String)

fun libraryFunction(): @Anno @AnnoWithArgs("") List<@Anno @AnnoWithArgs("") String> = TODO()

// MODULE: main(library)
// MODULE_KIND: Source
// FILE: main.kt
import library.libraryFunction

fun test() {
    val value = <expr>libraryFunction()</expr>
}
