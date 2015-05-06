fun foo(p: Int, handler: ((String, Char) -> Unit)?){}

fun bar(handler: (String, Char) -> Unit) {
    foo(1)<caret>
}

// EXIST: "{ String, Char -> ... }"
// ABSENT: null
// ABSENT: handler