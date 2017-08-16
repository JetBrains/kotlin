fun foo(s: String) {}

fun bar(s: String?) {
    if (s == null) {
        1
    }
    else if (<caret>true) {
        foo("a")
        foo("b")
        2
    }
    else {
        3
    }
}