// FILE: lib.kt
package lib

@Target(AnnotationTarget.FUNCTION)
annotation class Anno(val x: String = "foo")

// FILE: main.kt
import lib.Anno

@Anno
fun foo(): Int = 5

@Anno(x = "bar")
fun bar(): Int = 10

fun test() {
    <caret>val x = 0
}