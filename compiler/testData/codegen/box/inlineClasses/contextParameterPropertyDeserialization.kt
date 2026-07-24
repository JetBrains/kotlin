// ISSUE: KT-87290
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ContextParameters

// MODULE: lib
// FILE: lib.kt
OPTIONAL_JVM_INLINE_ANNOTATION
value class Wrapper(val value: Int) {
    context(prefix: String)
    val value: String
        get() = prefix
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class Wrapper1(val value: Int) {
    context(prefix: String)
    private val value: String
        get() = prefix
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class Wrapper2(private val value: Int) {
    context(prefix: String)
    val value: String
        get() = prefix
}

// MODULE: main(lib)
// FILE: main.kt
fun box(): String {
    val w0 = Wrapper(42)
    if (w0.value != 42) return w0.value.toString()

    val w1 = Wrapper1(42)
    if (w1.value != 42) return w1.value.toString()
    val w2 = Wrapper2(42)

    context("SOME") {
        if (w1.value != 42) return w1.value.toString()
        if (w2.value != "SOME") return w2.value
    }

    return "OK"
}
