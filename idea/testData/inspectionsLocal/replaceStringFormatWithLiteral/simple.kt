// WITH_RUNTIME
// HIGHLIGHT: INFORMATION

fun test() {
    val foo = 1

    <caret>String.format("foo is %s, bar is %s.", foo, Bar().value)
}

class Bar {
    val value = 2
}