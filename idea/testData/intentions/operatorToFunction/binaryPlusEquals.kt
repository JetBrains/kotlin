class Bar {
    fun plusAssign(arg: Bar) {}
}

fun foo(b: Bar) {
    var a = Bar()
    a <caret>+= b
}
