// IGNORE_FE10
// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: lib.kt

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

// MODULE: main(lib)
// FILE: usage.kt

class Test : Outer<String>() {
    val inn1_1: Inner = createInner()
    val inn1_2: Outer<Boolean>.Inner = Outer<Boolean>().createInner()

    val wrong: Outer<Boolean>.Inner = createInner()

    val inn2_1: Nested2<Short>.Inner2<Int> = createInner2()
    val inn2_2: Outer<Boolean>.Nested2<Short>.Inner2<Int> = Outer<Boolean>().createInner2()

    val inn3_1: Nested3<Short>.Inner3<Int> = createInner3()
    val inn3_2: Outer.Nested3<Short>.Inner3<Int> = Outer<Boolean>().createInner3()
}
