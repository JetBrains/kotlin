fun foo(a: Int, b: String, c: String = "", d: Int= 0) {}

fun bar(b: String, a: Int, c: String, d: Int) {
    foo(<caret>)
}

// EXIST: "a, b"
// EXIST: "a, b, c"
// EXIST: "a, b, c, d"
