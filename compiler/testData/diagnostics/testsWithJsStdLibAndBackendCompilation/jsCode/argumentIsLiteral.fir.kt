val a = "1"

fun nonConst(): String = "1"

fun test() {
    val b = "b"

    js(a)
    js((b))
    js(("c"))
    js(<!ARGUMENT_TYPE_MISMATCH!>3<!>)
    js(<!ARGUMENT_TYPE_MISMATCH!>3 + 2<!>)
    js(<!ARGUMENT_TYPE_MISMATCH!>1.0f<!>)
    js(<!ARGUMENT_TYPE_MISMATCH!>true<!>)
    js("$a")
    js("${1}")
    js("$b;")
    js("${b}bb")
    js(a + a)
    js("a" + "a")
    js("ccc")

    js(nonConst())
}
