// PROBLEM: none

fun foo(a: Boolean, b: Boolean): String {
    var res = ""
    if (a) {

    }
    else <caret>if (b) {
        res += "b"
    }
    else {
        res += "!b"
    }
    return res
}