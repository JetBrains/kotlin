// WITH_STDLIB
// MODULE: main
// FILE: main.kt

/**
 * Foo KDoc
 *
 * @param T type parameter
 * @param a value of type [T]
 */
class Foo<T>(val a: T)

/**
 * Function KDoc
 *
 * @param a parameter
 * @return nothing
 */
fun foo(a: String) {}