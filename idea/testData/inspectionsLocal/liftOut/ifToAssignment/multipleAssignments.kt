// PROBLEM: none

fun foo(x: Boolean): String {
    var s = ""
    <caret>if (x) {
        s += "a"
        s += "b"
    } else {
        s += "c"
    }
    return s
}