// MODULE: library
// MODULE_KIND: LibraryBinary
// FILE: constants.kt
package library

const val MY_INT = 3
const val MY_STRING = "str"

enum class E { A, B }

// FILE: library.kt
package library

import kotlin.reflect.KClass

@Target(AnnotationTarget.TYPE)
annotation class Nested(val i: Int = 0, vararg val classes: KClass<*>)

@Target(AnnotationTarget.TYPE)
annotation class Complex(
    val template: String,
    val negative: Int,
    val unsigned: UInt,
    val enums: Array<E>,
    val nestedArray: Array<Nested>,
    vararg val x: Int,
)

fun libraryFunction(): @Complex(
    template = "a${MY_STRING}b$MY_INT",
    negative = -MY_INT,
    unsigned = MY_INT.toUInt() + 4u,
    enums = [E.A, E.B],
    nestedArray = [Nested(classes = [String::class]), Nested(i = MY_INT, classes = *arrayOf(Int::class, E::class))],
    x = *intArrayOf(MY_INT, MY_INT + 1),
) String = TODO()

// MODULE: main(library)
// MODULE_KIND: Source
// FILE: main.kt
import library.libraryFunction

fun test() {
    val value = <expr>libraryFunction()</expr>
}
