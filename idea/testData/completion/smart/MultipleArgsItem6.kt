fun foo(a: Int, b: String, c: String) {}

fun bar(b: String, a: Int, c: String?) {
    if (c != null) {
        foo(<caret>)
    }
}

// EXIST: "a, b, c"
