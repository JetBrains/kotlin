// OPTIONS: overloadUsages
open class A<T> {
    open fun <caret>foo(t: T) {
        println(t)
    }

    open fun foo(t: T, tt: T) {
        println(t)
    }
}

fun <T> A<T>.foo(t: T, x: String) {
    foo(t)
    println(x)
}

fun A<String>.foo(s: String, n: Number) {
    fun <T> A<T>.foo(t: T, x: String) {
        foo(t)
        println(x)
    }

    foo(s)
    println(n)
}