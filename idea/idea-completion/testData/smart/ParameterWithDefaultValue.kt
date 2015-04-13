fun foo(p1: String, p2: String = "") { }

fun bar(s: String) {
    foo("", <caret>)
}

// EXIST: s
