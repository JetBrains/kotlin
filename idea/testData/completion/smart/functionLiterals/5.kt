fun foo(p: String.(Int) -> Unit){}

fun bar() {
    foo(<caret>)
}

// EXIST: "{...}"
// EXIST: "{ Int -> ... }"
