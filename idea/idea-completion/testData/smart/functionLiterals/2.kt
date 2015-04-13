fun foo(p: (String) -> Unit){}

fun bar() {
    foo(<caret>)
}

// EXIST: "{...}"
// EXIST: "{ String -> ... }"
