// RUNTIME_WITH_FULL_JDK

import java.lang.String.format

fun test() {
    val foo = 1
    val bar = 2

    <caret>format("foo is %s, bar is %s.", foo, bar)
}