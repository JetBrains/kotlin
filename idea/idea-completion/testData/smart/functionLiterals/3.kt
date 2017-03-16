fun foo(p: (String, Int) -> Unit){}

fun bar() {
    foo(<caret>)
}

// WITH_ORDER
// ABSENT: "{...}"
// EXIST: "{ s, i -> ... }"
// EXIST: "{ s: String, i: Int -> ... }"
