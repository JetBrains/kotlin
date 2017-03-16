fun foo(vararg args: Int, handler: (String, Char) -> Unit){}

fun bar(handler: (String, Char) -> Unit) {
    foo() <caret>
}

// EXIST: "{ s, c -> ... }"
// EXIST: "{ s: String, c: Char -> ... }"
// ABSENT: handler