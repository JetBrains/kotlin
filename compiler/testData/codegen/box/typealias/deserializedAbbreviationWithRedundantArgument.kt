// FIR_DUMP
// JVM_ABI_K1_K2_DIFF: KT-63864

// MODULE: a
// FILE: a.kt
typealias Foo<T> = List<String>

class C<T> {
    val foo: Foo<T>? = null
}

// MODULE: b(a)
// FILE: b.kt

val bar = C<Int>().foo

fun box() = "OK"