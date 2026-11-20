// ISSUE: KT-91636
// callable: one/Anno.value
// MODULE: library
// MODULE_KIND: LibraryBinary
// FILE: library.kt
package one

import kotlin.reflect.KClass

annotation class Anno(val value: KClass<out Number> = Int::class)

// MODULE: main(library)
// FILE: main.kt
fun main() {
}
