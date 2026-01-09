// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

// FILE: lib.kt
inline fun inlineFun(): String = "K"

// FILE: main.kt
OPTIONAL_JVM_INLINE_ANNOTATION
value class Foo(val a: String) {
    fun test(): String {
        return a + inlineFun()
    }
}

fun box(): String {
    val f = Foo("O")
    return f.test()
}