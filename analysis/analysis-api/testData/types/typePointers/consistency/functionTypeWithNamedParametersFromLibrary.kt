// MODULE: library
// MODULE_KIND: LibraryBinary
// FILE: library.kt
package library

fun libraryFunction(): (par1: String, @ParameterName("OneName") Int) -> Unit = TODO()

// MODULE: main(library)
// MODULE_KIND: Source
// FILE: main.kt
import library.libraryFunction

fun test() {
    val value = <expr>libraryFunction()</expr>
}
