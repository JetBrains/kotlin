// "Replace with 'new'" "true"
// WITH_RUNTIME

class A {
    @Deprecated("msg", ReplaceWith("new"))
    var old = ""
        get() = new

    var new = ""
}

fun foo() {
    val a = A()
    a.<caret>old = "foo"
}