fun foo(a: Int, b: String) {}

fun bar(a: Int, b: String) {
    fun local() {
        foo(<caret>)
    }
}

// EXIST: "a, b"
