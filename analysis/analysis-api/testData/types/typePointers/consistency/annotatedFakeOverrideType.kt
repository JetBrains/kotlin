// FILE: base.kt
@Target(AnnotationTarget.TYPE)
annotation class Anno(val number: Int)

private const val PRIVATE_CONST = 1

open class Base<T> {
    fun foo(): @Anno(PRIVATE_CONST) T = TODO()
}

// FILE: main.kt
private const val PRIVATE_CONST = 2

class Derived : Base<String>()

fun test() {
    <expr>Derived().foo()</expr>
}
