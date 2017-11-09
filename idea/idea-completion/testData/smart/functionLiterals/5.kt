fun foo(p: String.(Int) -> Unit){}

fun bar() {
    foo(<caret>)
}

// WITH_ORDER
// EXIST: "{...}"
// EXIST: "{ i -> ... }"
// EXIST: "{ i: Int -> ... }"
