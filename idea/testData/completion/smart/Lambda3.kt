fun foo(p: (String, Int) -> Unit){}

fun bar() {
    foo(<caret>)
}

// ABSENT: "{...}"
// EXIST: "{ (String, Int) -> ... }"
