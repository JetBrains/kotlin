fun foo(p: (x: Char, String) -> Unit){}

fun bar() {
    foo { <caret>}
}

// ELEMENT: "x, s ->"
