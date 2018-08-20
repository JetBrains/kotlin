// PROBLEM: none
// RUNTIME_WITH_FULL_JDK

import java.util.*

fun test() {
    val foo = Foo(1)
    val bar = 2

    <caret>String.format("foo is %s, bar is %s.", foo, bar)
}

class Foo(private val value: Int) : Formattable {
    override fun formatTo(formatter: Formatter?, flags: Int, width: Int, precision: Int) {
        formatter?.out()?.append("[$value]")
    }
}