val a = "1"

fun nonConst(): String = "1"

fun test() {
    val b = "b"

    js(a)
    js(b)
    js("$a")
    js("${1}")
    js("$b;")
    js("${b}bb")
    js(a + a)
    js("a" + "a")
    js("ccc")

    js(<!JSCODE_ARGUMENT_SHOULD_BE_CONSTANT!>nonConst()<!>)
}