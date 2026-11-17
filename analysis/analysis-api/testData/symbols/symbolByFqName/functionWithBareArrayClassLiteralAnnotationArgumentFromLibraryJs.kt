// ISSUE: KT-91636
// TARGET_PLATFORM: JS
// callable: one/foo
// MODULE: library
// MODULE_KIND: LibraryBinary
// FILE: library.kt
package one

import kotlin.reflect.KClass

annotation class Anno(val value: KClass<*>)

@Anno(Array::class)
fun foo() {
}

// MODULE: main(library)
// FILE: main.kt
fun main() {
}
