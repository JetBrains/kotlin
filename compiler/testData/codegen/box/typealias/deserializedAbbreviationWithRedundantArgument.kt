// IGNORE_IR_DESERIALIZATION_TEST: NATIVE
// ^^^ KT-73619: dump of dependent Klib is missing. Should be fixed after KT-73619
// FIR_DUMP

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