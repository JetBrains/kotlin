// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

// FILE: lib.kt
inline fun inlineFun(): String = "K"

// FILE: main.kt
OPTIONAL_JVM_INLINE_ANNOTATION
value class Foo<T: String>(val a: T) {
    fun test(): String {
        return a + inlineFun()
    }
}

fun box(): String {
    val f = Foo("O")
    return f.test()
}