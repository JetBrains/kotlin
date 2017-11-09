// PROBLEM: none

fun foo(p: Boolean): String {
    if (p) {
        <caret>return "abc"
    }
    else {
        return "def"
    }
}