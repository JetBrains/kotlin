fun foo(s: String, c: Char){}

fun bar(p1: String?, p2: String) {
    foo(p1 ?: <caret>
}

// ELEMENT: p2
