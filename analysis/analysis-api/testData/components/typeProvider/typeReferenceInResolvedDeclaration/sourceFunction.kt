// MODULE: lib
// FILE: lib.kt
package lib

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE)
annotation class Anno

@Anno
fun <T : Comparable<T>> @receiver:Anno String.foo(@Anno p: T, q: List<@Anno String>, f: (Int) -> Unit): Map<String, T?> = TODO()

// MODULE: main(lib)
// FILE: main.kt
import lib.foo

fun test() {
    "".f<caret>oo(1, emptyList()) {}
}
