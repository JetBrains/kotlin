val a = "1"

fun nonConst(): String = "1"

fun test() {
    val b = "b"

    js(a)
    js((b))
    js(("c"))
    js(<!CONSTANT_EXPECTED_TYPE_MISMATCH, JSCODE_ARGUMENT_SHOULD_BE_CONSTANT!>3<!>)
    js(<!JSCODE_ARGUMENT_SHOULD_BE_CONSTANT, TYPE_MISMATCH!>3 + 2<!>)
    js(<!CONSTANT_EXPECTED_TYPE_MISMATCH, JSCODE_ARGUMENT_SHOULD_BE_CONSTANT!>1.0f<!>)
    js(<!CONSTANT_EXPECTED_TYPE_MISMATCH, JSCODE_ARGUMENT_SHOULD_BE_CONSTANT!>true<!>)
    js("$a")
    js("${1}")
    js("$b;")
    js("${b}bb")
    js(a + a)
    js("a" + "a")
    js("ccc")

    js(<!JSCODE_ARGUMENT_SHOULD_BE_CONSTANT!>nonConst()<!>)
}