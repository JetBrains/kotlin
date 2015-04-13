fun foo(a: Int, b: String) {}

fun f(p: (Int, String) -> Unit){}

fun bar() {
    f { (a, b) -> foo(<caret>) }
}

// EXIST: "a, b"
