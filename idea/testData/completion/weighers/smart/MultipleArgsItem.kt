fun foo(a: Int, b: String, c: String) {}

fun bar(b: String, a: Int, c: String) {
    foo(<caret>)
}

// ORDER: "a, b, c"
// ORDER: a
