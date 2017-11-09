fun foo(p: Int, flag: Boolean, x: Int){}
fun foo(p: Int, flag: Boolean?, y: Char){}

fun bar() {
    foo(1, <caret>)
}

// ELEMENT_TEXT: "flag = true"
