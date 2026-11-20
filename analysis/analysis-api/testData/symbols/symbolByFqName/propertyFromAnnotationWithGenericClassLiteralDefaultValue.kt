// ISSUE: KT-91636
// callable: one/Anno.value
// MODULE: library
// MODULE_KIND: LibraryBinary
// FILE: library.kt
package one

import kotlin.reflect.KClass

class Box<T>

annotation class Anno(val value: KClass<*> = Box::class)

// MODULE: main(library)
// FILE: main.kt
fun main() {
}
