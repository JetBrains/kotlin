fun foo(vararg args: Int, handler: (String, Char) -> Unit){}

fun bar(handler: (String, Char) -> Unit) {
    foo(1, 2) <caret>
}

// EXIST: "{ String, Char -> ... }"
// ABSENT: handler