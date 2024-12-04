// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtClass
// FILE: usage.kt

// KT-70657
open class Outer<T> {
    inner class Inner

    fun createInner(): Inner = Inner()



    inner class Nested2<U> {
        inner class Inner2<V>
    }

    fun createInner2(): Nested2<Short>.Inner2<Int> = throw Exception()



    // not inner!
    class Nested3<U> {
        inner class Inner3<V>
    }

    fun createInner3(): Nested3<Short>.Inner3<Int> = throw Exception()
}
