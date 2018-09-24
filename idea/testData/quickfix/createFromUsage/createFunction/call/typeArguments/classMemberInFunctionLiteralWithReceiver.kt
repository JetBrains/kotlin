// "Create member function 'T.bar'" "true"
open class X

fun <T : X> foo(t: T, f: T.() -> Unit = {}) {}

class Text<T : X>(private val t: T) {
    fun f() = foo(t) { <caret>bar() }
}