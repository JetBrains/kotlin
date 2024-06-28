// FIR_IDENTICAL

fun testSimpleString() {
    js("var a = 123;")
}

fun testSimpleStringPlus() {
    js("var a" + "=" + "123;")
}

fun testSimpleStringConcat() {
    js("var a${"="}123;")
}
