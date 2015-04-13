fun foo(p1: Int, p2: Int) { }

fun bar(pp: Int, a: Int) {
    foo(<caret>)
}

// COMPLETION_TYPE: SMART
// ELEMENT: pp
// CHAR: ','
