fun foo(s: String) {}

fun bar(s: String?) {
    if (s == null) {
        foo("a")
        foo("b")
    }
    else if (<caret>true) {
    }
    else {
        foo("c")
        foo("d")
    }
}