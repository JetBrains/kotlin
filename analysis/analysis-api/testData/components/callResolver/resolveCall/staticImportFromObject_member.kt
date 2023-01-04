// FILE: call.kt
import Foo.bar

fun test() {
    <expr>bar()</expr>
}

// FILE: Foo.kt

object Foo {
    fun bar() {}
}