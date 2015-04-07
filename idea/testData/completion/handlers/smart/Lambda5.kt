fun foo(p: (String, StringBuilder) -> Unit){}
fun foo(p: (Int, Char) -> Unit){}

fun bar() {
    foo(<caret>)
}

// ELEMENT: "{ String, StringBuilder -> ... }"
