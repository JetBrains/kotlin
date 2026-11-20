// ISSUE: KT-91727, KT-91636
// callable: one/foo
// MODULE: library
// MODULE_KIND: LibraryBinary
// FILE: library.kt
package one

import kotlin.reflect.KClass

class Box<T>

annotation class Anno(val value: KClass<*>, vararg val values: KClass<*>)

@Anno(
    Array<String>::class,
    IntArray::class,
    Array<IntArray>::class,
    Array<Array<String>>::class,
    Box::class,
)
fun foo() {
}

// MODULE: main(library)
// FILE: main.kt
fun main() {
}
