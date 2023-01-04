// FILE: call.kt
import Foo.bar

fun test() {
    <expr>"42".bar()</expr>
}

// FILE: Foo.kt

object Foo {
    fun String.bar(): Int = this.length
}