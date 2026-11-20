// ISSUE: KT-91636, KT-91727
// callable: one/Anno.value
// MODULE: library
// MODULE_KIND: LibraryBinary
// FILE: library.kt
package one

import kotlin.reflect.KClass

annotation class Anno(val value: KClass<*> = Array<Array<String>>::class)

// MODULE: main(library)
// FILE: main.kt
fun main() {
}
