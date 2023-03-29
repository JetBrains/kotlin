// FIR_IDENTICAL

// MUTE_SIGNATURE_COMPARISON_K2: JS_IR
// MUTE_SIGNATURE_COMPARISON_K2: NATIVE
// ^ KT-57818

class A<T> {
    fun foo() {}
    val bar = 42
}

val test1 = A<String>::foo
val test2 = A<String>::bar
