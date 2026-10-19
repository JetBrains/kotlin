// ISSUE: KT-87290
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +CompanionBlocks
// IGNORE_BACKEND: JVM
//                 ^^^ CONFLICTING_JVM_DECLARATIONS for <get-value>

// MODULE: lib
// FILE: lib.kt
OPTIONAL_JVM_INLINE_ANNOTATION
value class Wrapper(val value: Int) {
    context(prefix: String)
    val value: String
        get() = prefix

    companion {
        val value get() = 1
    }
}

// MODULE: main(lib)
// FILE: main.kt
fun box(): String {
    val w = Wrapper(42)
    if (w.value != 42) return "fail 1: ${w.value}"

    if (Wrapper.value != 1) return "fail 2: ${Wrapper.value}"

    return "OK"
}
