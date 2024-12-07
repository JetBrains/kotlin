// FILE: Anno.kt
package p3

@Target(AnnotationTarget.FUNCTION)
annotation class Anno(vararg val x: String)

// FILE: main.kt
import p3.Anno

@Anno("A", "B")
fun foo(): Int = 10

fun test() {
    val x = foo()
}