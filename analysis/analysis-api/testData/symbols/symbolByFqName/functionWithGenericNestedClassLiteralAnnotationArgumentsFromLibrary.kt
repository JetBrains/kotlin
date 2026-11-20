// ISSUE: KT-91636
// callable: one/foo
// MODULE: library
// MODULE_KIND: LibraryBinary
// FILE: library.kt
package one

import kotlin.reflect.KClass

class Outer<T> {
    inner class Inner<U>

    inner class InnerWithoutParameters

    class Nested<V>
}

annotation class Anno(vararg val values: KClass<*>)

@Anno(Outer::class, Outer.Inner::class, Outer.InnerWithoutParameters::class, Outer.Nested::class)
fun foo() {
}

// MODULE: main(library)
// FILE: main.kt
fun main() {
}
