// FILE: a.kt
package a

@Target(AnnotationTarget.TYPE)
annotation class Anno(val number: Int)

private const val value = 1

fun typeWithAnnotation(): @Anno(value) String = ""

// FILE: b.kt
package b
import a.typeWithAnnotation

private const val value = 2

val resolveMe = typeWithAnnotation()
fun foo() {
    <expr>resolveMe</expr>
}
