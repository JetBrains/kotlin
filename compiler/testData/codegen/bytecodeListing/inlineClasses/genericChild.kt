// WITH_STDLIB
inline class X(val x: Any?)

interface IFoo<out T : X?> {
    fun foo(): T
}

fun <T : X> foo(x: T) {}

