fun foo(p: (String, StringBuilder) -> Unit){}
fun foo(p: (String) -> Unit){}

fun bar() {
    foo(<caret>)
}

// ELEMENT: "{ String, StringBuilder -> ... }"
