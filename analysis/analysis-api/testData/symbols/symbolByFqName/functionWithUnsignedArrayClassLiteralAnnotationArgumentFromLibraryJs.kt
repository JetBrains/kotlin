// ISSUE: KT-91728
// TARGET_PLATFORM: JS
// callable: one/foo
// MODULE: library
// MODULE_KIND: LibraryBinary
// FILE: library.kt
package one

import kotlin.reflect.KClass

annotation class Anno(val value: KClass<*>)

@Anno(UIntArray::class)
fun foo() {
}

// MODULE: main(library)
// FILE: main.kt
fun main() {
}
