// "Remove parameter 'x'" "false"
// ACTION: Add 'block =' to argument
// ACTION: Convert parameter to receiver
// ACTION: Rename to _
// ACTION: Specify return type explicitly
// ACTION: Put parameters on separate lines

fun foo(block: (String, Int) -> Unit) {
    block("", 1)
}

fun bar() {
    foo(fun(x<caret>: String, y: Int) = Unit)
}
