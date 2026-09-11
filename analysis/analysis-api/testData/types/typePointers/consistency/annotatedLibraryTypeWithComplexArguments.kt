// MODULE: library
// MODULE_KIND: LibraryBinary
// FILE: library.kt
package library

import kotlin.reflect.KClass

enum class E { A, B }

@Target(AnnotationTarget.TYPE)
annotation class Nested(val i: Int)

@Target(AnnotationTarget.TYPE)
annotation class Complex(
    val s: String,
    val i: Int,
    val l: Long,
    val b: Boolean,
    val c: Char,
    val d: Double,
    val e: E,
    val k: KClass<*>,
    val strings: Array<String>,
    val enums: Array<E>,
    val classes: Array<KClass<*>>,
    val nested: Nested,
)

fun libraryFunction(): @Complex(
    s = "s",
    i = 1 + 2,
    l = 42L,
    b = true,
    c = 'c',
    d = 1.5,
    e = E.B,
    k = List::class,
    strings = ["a", "b"],
    enums = arrayOf(E.A, E.B),
    classes = [Int::class, Array<String>::class],
    nested = Nested(7),
) List<String> = TODO()

// MODULE: main(library)
// MODULE_KIND: Source
// FILE: main.kt
import library.libraryFunction

fun test() {
    val value = <expr>libraryFunction()</expr>
}
